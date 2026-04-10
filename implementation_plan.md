# Implementation Plan - 2-Factor Authentication (2FA) via Email (Updated)

This plan outlines the updated steps to implement 2FA using email for the ChriOnline application.

## User Review Required

> [!IMPORTANT]
> The 2FA code will NOT be stored in the database. Instead, it will be kept in a transient in-memory map on the server. This means if the server restarts, any pending 2FA sessions will be lost.
> The existing `EmailService.java` at `ma.ensate.util` will be used for sending codes.

## Proposed Changes

### Database Layer

#### [MODIFY] [database_basket.sql](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/database_basket.sql)
- Add a column to the `utilisateur` table:
    - `two_factor_enabled`: TINYINT (0 or 1, default 0)

---

### Model & DAO Layer

#### [MODIFY] [Utilisateur.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/models/Utilisateur.java)
- Add `private boolean twoFactorEnabled;`
- Add corresponding getter and setter.

#### [MODIFY] [UtilisateurDAO.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java)
- Update `trouverParEmailPassword` and `trouverParId` to fetch `two_factor_enabled`.
- Update `mettreAJourProfil` to support updating `two_factor_enabled`.

---

### Backend Services (Server)

#### [MODIFY] [UserService.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/server/services/UserService.java)
- Define an internal class `PendingMFA`:
  ```java
  private static class PendingMFA {
      String code;
      long expiry;
      int attempts;
  }
  ```
- Add a static map to store codes in memory:
  ```java
  private static final Map<Integer, PendingMFA> mfaBuffer = new ConcurrentHashMap<>();
  ```
- Update `login(data, clientIP)`:
    - After password validation, check `u.isTwoFactorEnabled()`.
    - If true:
        - Generate a 6-digit random code.
        - Create a `PendingMFA` object (expiry = 10 mins).
        - Store it in `mfaBuffer` with key `u.getId()`.
        - Send the code email using `new EmailService().sendHtml(...)`.
        - Return `Response(true, "2FA_REQUIRED", u.getId())`.
- Add `verify2FA(Object data)`:
    - Data will be an `Object[]` containing `[userId, code]`.
    - Retrieve `PendingMFA` from `mfaBuffer`.
    - Check if it exists and hasn't expired.
    - Increment `attempts`.
    - If code matches and `attempts <= 3`:
        - Generate session token.
        - Remove from `mfaBuffer`.
        - Return success with the `Utilisateur` object.
    - Else if `attempts >= 3`:
        - Remove from `mfaBuffer`.
        - Return error "Nombre maximum de tentatives atteint".

#### [MODIFY] [ClientHandler.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/server/network/ClientHandler.java)
- Register the new action `VERIFY_2FA` in the `traiterRequete` switch.

---

### Frontend UI (Client)

#### [MODIFY] [ProfilView.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/client/views/ProfilView.java)
- Add a `CheckBox` for 2FA.
- Update `validerModifications` to send the checkbox state.

#### [MODIFY] [LoginView.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/client/views/LoginView.java)
- Handle the `2FA_REQUIRED` message by displaying a popup or switching to a verification view.

#### [NEW] [TwoFactorVerifyView.java](file:///c:/ENSA/Génie%20Informatique/GI%202/S8/Sécurité%20Informatique/projets/src/main/java/ma/ensate/client/views/TwoFactorVerifyView.java)
- Create a simple scene with a `TextField` for the code and a "Vérifier" button.

## Verification Plan

### Manual Verification
1.  **Enable 2FA**: Settings -> Profile -> Check 2FA -> Save.
2.  **Trigger 2FA**: Logout and Login. Confirm that a code is sent to the email and the 2FA view appears.
3.  **Invalid Code**: Enter a wrong code. Verify error and attempt count increment.
4.  **Max Attempts**: Enter wrong code 3 times. Verify it resets/fails and prevents further entry of that code.
5.  **Success**: Enter the correct code on the next login. Verify successful redirection to the main app.
