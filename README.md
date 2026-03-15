# ChriOnline 🛒 — E-Commerce Java Sockets

ChriOnline est une application de commerce électronique native basée sur une architecture **Client/Serveur** utilisant les Sockets Java (TCP/UDP). Ce projet s'inscrit dans le cadre du module **SSI-AT (2026)**.

## 🎯 Objectif du Projet
Développer une solution complète d'achat en ligne mettant en œuvre la programmation réseau multi-clients, la gestion des sessions sécurisées et la persistance des données via JDBC.

## 🚀 Fonctionnalités

### 📋 Niveau Minimum (Fondamentaux)
- **Authentification** : Inscription et connexion sécurisée des utilisateurs.
- **Catalogue** : Consultation de la liste des produits et affichage des détails (nom, prix, description, stock).
- **Panier** : Ajout/suppression de produits et calcul automatique du montant total.
- **Commandes** : Validation des achats avec génération d'un identifiant unique.
- **Paiement** : Système de paiement simulé (CB ou paiement fictif).

### 🌟 Niveau Avancé (Interface & Administration)
- **Interface Graphique (GUI)** : Design ergonomique réalisé avec JavaFX.
- **Profil Utilisateur** : Historique complet des commandes et gestion du profil.
- **Gestion des Stocks** : Mise à jour automatique des quantités après chaque achat.
- **Système de Notions** : Notifications de confirmation (via UDP).
- **Interface Administrateur** : 
  - CRUD Complet des produits (Ajouter, Modifier, Supprimer).
  - Gestion globale des commandes et des statuts (en attente, validée, expédiée, livrée).
  - Gestion des comptes utilisateurs.

## 🛠️ Spécifications Techniques

- **Cœur** : Java 17+ (Multi-threading côté serveur).
- **Communication** : 
  - **TCP** : Opérations critiques (Auth, Panier, Commandes).
  - **UDP** : Notifications rapides.
- **Base de Données** : MySQL (Accès via JDBC).
- **Interface** : JavaFX pour un rendu moderne et "premium".
- **Sécurité** : Gestion des sessions via tokens UUID.

## 🏗️ Architecture du Projet

Le projet est structuré selon les meilleures pratiques pour faciliter la collaboration :
- **`common/`** : Protocoles de communication (`Request`, `Response`) et DTOs partagés.
- **`server/`** : Logique métier, DAOs, et gestion des threads clients.
- **`client/`** : Vues FXML, contrôleurs et gestion de la session locale.

👉 Consultez [Détails de l'Architecture & Workflow](./architecture_workflow.md) pour plus d'infos techniques.

## 👥 Équipe de Développement

| Membre | Rôle / Responsabilité |
| :--- | :--- |
| **Nouha ERRABOUN** | Sécurité, Squelette Serveur, Authentification |
| **Ismail LYAMANI** | Gestion des Produits, Catalogue, Vue Détails |
| **Alae EL BARKOUKI** | Gestion du Panier & Logique d'Achat |
| **Jihane EL HAMDAOUI** | Gestion des Commandes & Historique |

## 📄 Licence
Ce projet est sous licence MIT. Voir le fichier [LICENSE](./LICENSE).
