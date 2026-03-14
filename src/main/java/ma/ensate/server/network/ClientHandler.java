package ma.ensate.server.network;

import ma.ensate.models.*;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.*;
import ma.ensate.server.dao.ClientDAO;
import ma.ensate.server.dao.ProduitDAO;
import ma.ensate.server.services.CommandeService;
import ma.ensate.server.services.PaymentService;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère les connexions TCP des clients
 * Route les requêtes vers les services appropriés
 */
public class ClientHandler extends Thread {
    
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private CommandeService commandeService;
    private PaymentService paymentService;
    private ClientDAO clientDAO;
    private ProduitDAO produitDAO;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.commandeService = new CommandeService();
        this.paymentService = new PaymentService();
        this.clientDAO = new ClientDAO();
        this.produitDAO = new ProduitDAO();
    }

    @Override
    public void run() {
        try {
            // Initialiser les flux d'entrée/sortie
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
            
            System.out.println("Client connecté: " + clientSocket.getInetAddress());

            // Boucle principale de traitement des requêtes
            while (!clientSocket.isClosed()) {
                try {
                    Request request = (Request) input.readObject();
                    System.out.println("Requête reçue: " + request.getAction());
                    
                    Response response = traiterRequete(request);
                    
                    output.writeObject(response);
                    output.flush();
                    
                } catch (EOFException e) {
                    // Client déconnecté
                    break;
                } catch (ClassNotFoundException e) {
                    sendError("Erreur: classe non trouvée - " + e.getMessage());
                } catch (Exception e) {
                    sendError("Erreur lors du traitement: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            System.out.println("Erreur de communication avec le client: " + e.getMessage());
        } finally {
            fermerConnexion();
        }
    }

    /**
     * Traite une requête et retourne une réponse
     */
    private Response traiterRequete(Request request) {
        String action = request.getAction();
        
        try {
            switch (action) {
                // Actions de commande
                case "CREER_COMMANDE":
                    return creerCommande(request);
                    
                case "VALIDER_COMMANDE":
                    return validerCommande(request);
                    
                case "CHANGER_STATUT_COMMANDE":
                    return changerStatutCommande(request);
                    
                case "GET_COMMANDE":
                    return getCommande(request);
                    
                case "GET_HISTORIQUE":
                    return getHistorique(request);
                    
                // Actions de paiement
                case "EFFECTUER_PAIEMENT":
                    return effectuerPaiement(request);
                    
                case "GET_PAIEMENT":
                    return getPaiement(request);
                    
                default:
                    return new Response(false, "Action inconnue: " + action);
            }
        } catch (Exception e) {
            return new Response(false, "Erreur: " + e.getMessage());
        }
    }

    // ========== ACTIONS DE COMMANDE ==========

    private Response creerCommande(Request request) {
        try {
            CreerCommandeRequest req = (CreerCommandeRequest) request.getData();
            
            // Validation
            if (req == null || req.getLignes() == null || req.getLignes().isEmpty()) {
                return new Response(false, "La requête doit contenir des lignes de commande");
            }
            
            if (req.getClientId() <= 0) {
                return new Response(false, "ID client invalide");
            }
            
            // Récupérer le client
            Client client = clientDAO.findById(req.getClientId());
            if (client == null) {
                return new Response(false, "Client introuvable avec l'ID: " + req.getClientId());
            }
            
            // Convertir les DTOs en LigneCommande
            List<LigneCommande> lignes = convertirLignesCommande(req.getLignes());
            if (lignes.isEmpty()) {
                return new Response(false, "Aucune ligne de commande valide");
            }
            
            // Créer la commande
            Commande commande = commandeService.creerCommande(client, lignes);
            
            return new Response(true, "Commande créée avec succès", commande);
            
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        } catch (Exception e) {
            return new Response(false, "Erreur: " + e.getMessage());
        }
    }

    private Response validerCommande(Request request) {
        try {
            String commandeId = (String) request.getData();
            
            if (commandeId == null || commandeId.trim().isEmpty()) {
                return new Response(false, "ID de commande requis");
            }
            
            boolean success = commandeService.validerCommande(commandeId);
            
            if (success) {
                return new Response(true, "Commande validée avec succès");
            } else {
                return new Response(false, "Échec de la validation de la commande");
            }
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    private Response changerStatutCommande(Request request) {
        try {
            ChangerStatutRequest req = (ChangerStatutRequest) request.getData();
            
            if (req == null || req.getCommandeId() == null || req.getNouveauStatut() == null) {
                return new Response(false, "Requête invalide");
            }
            
            // Convertir le statut string en enum
            StatutCommande nouveauStatut;
            try {
                nouveauStatut = StatutCommande.valueOf(req.getNouveauStatut());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Statut invalide: " + req.getNouveauStatut());
            }
            
            boolean success = commandeService.changerStatutCommande(req.getCommandeId(), nouveauStatut);
            
            if (success) {
                return new Response(true, "Statut de la commande mis à jour avec succès");
            } else {
                return new Response(false, "Échec de la mise à jour du statut");
            }
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    private Response getCommande(Request request) {
        try {
            String commandeId = (String) request.getData();
            
            if (commandeId == null || commandeId.trim().isEmpty()) {
                return new Response(false, "ID de commande requis");
            }
            
            Commande commande = commandeService.getCommandeById(commandeId);
            
            if (commande != null) {
                return new Response(true, "Commande trouvée", commande);
            } else {
                return new Response(false, "Commande introuvable");
            }
            
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    private Response getHistorique(Request request) {
        try {
            Integer clientId = (Integer) request.getData();
            
            if (clientId == null || clientId <= 0) {
                return new Response(false, "ID client invalide");
            }
            
            List<Commande> historique = commandeService.getHistorique(clientId);
            
            return new Response(true, "Historique récupéré", historique);
            
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    // ========== ACTIONS DE PAIEMENT ==========

    private Response effectuerPaiement(Request request) {
        try {
            PaiementRequest req = (PaiementRequest) request.getData();
            
            // Validation
            if (req == null || req.getCommandeId() == null || req.getMethodePaiement() == null) {
                return new Response(false, "Requête de paiement invalide");
            }
            
            // Validation de la méthode de paiement
            MethodePaiement methode;
            try {
                methode = MethodePaiement.valueOf(req.getMethodePaiement());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Méthode de paiement invalide: " + req.getMethodePaiement());
            }
            
            // Validation pour carte bancaire
            if (methode == MethodePaiement.CARTE_BANCAIRE) {
                if (req.getCardLast4() == null || req.getCardLast4().length() != 4) {
                    return new Response(false, "Les 4 derniers chiffres de la carte sont requis pour le paiement par carte");
                }
            }
            
            // Récupérer la commande
            Commande commande = commandeService.getCommandeById(req.getCommandeId());
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }
            
            // Effectuer le paiement
            boolean success = paymentService.effectuerPaiement(commande, methode, req.getCardLast4());
            
            if (success) {
                // Récupérer le paiement créé
                Paiement paiement = paymentService.getPaiementByCommandeId(req.getCommandeId());
                return new Response(true, "Paiement effectué avec succès", paiement);
            } else {
                return new Response(false, "Échec du paiement");
            }
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    private Response getPaiement(Request request) {
        try {
            String commandeId = (String) request.getData();
            
            if (commandeId == null || commandeId.trim().isEmpty()) {
                return new Response(false, "ID de commande requis");
            }
            
            Paiement paiement = paymentService.getPaiementByCommandeId(commandeId);
            
            if (paiement != null) {
                return new Response(true, "Paiement trouvé", paiement);
            } else {
                return new Response(false, "Paiement introuvable pour cette commande");
            }
            
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données: " + e.getMessage());
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Convertit les DTOs en objets LigneCommande
     */
    private List<LigneCommande> convertirLignesCommande(List<LigneCommandeDTO> dtos) throws SQLException {
        List<LigneCommande> lignes = new ArrayList<>();
        
        for (LigneCommandeDTO dto : dtos) {
            // Validation
            if (dto.getProduitId() <= 0 || dto.getQuantite() <= 0) {
                continue; // Ignorer les lignes invalides
            }
            
            // Récupérer le produit
            Produit produit = produitDAO.findById(dto.getProduitId());
            if (produit == null) {
                throw new IllegalArgumentException("Produit introuvable avec l'ID: " + dto.getProduitId());
            }
            
            // Créer la ligne de commande
            LigneCommande ligne = new LigneCommande(produit, dto.getQuantite());
            lignes.add(ligne);
        }
        
        return lignes;
    }

    /**
     * Envoie une réponse d'erreur au client
     */
    private void sendError(String message) {
        try {
            Response response = new Response(false, message);
            output.writeObject(response);
            output.flush();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi de la réponse: " + e.getMessage());
        }
    }

    /**
     * Ferme la connexion avec le client
     */
    private void fermerConnexion() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("Connexion fermée: " + clientSocket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
    }
}
