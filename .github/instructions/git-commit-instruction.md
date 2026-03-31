### Instructions Git optimisées

---

### 1. **Structure du message de commit**

#### 1.1. **En-tête**
- Format : `<type>[optional scope]: <short description>`
    - **type** : Mot-clé décrivant le type de changement (ex. : feat, fix, chore, docs, style, refactor, perf, test, ci).
    - **scope** (optionnel) : Nom décrivant la section du code (ex. : auth, router, service).
    - **description** : Résumé impératif, concis, sans point final.

#### 1.2. **Corps** (optionnel)
- Expliquez la motivation et les changements apportés.
- Utilisez un ton impératif au présent (ex. : "change" au lieu de "changed").
- Limitez chaque ligne à ~72 caractères pour une meilleure lisibilité.

#### 1.3. **Pied de page** (optionnel)
- Ajoutez des métadonnées comme des références à des issues ou des notes de rupture.
    - Exemple : `Closes #123`.
    - Pour des changements majeurs : `BREAKING CHANGE: <description>` ou ajoutez `!` après le type/scope.

---

### 2. **Types courants et leur impact SemVer**

| **Type**   | **Impact SemVer**                     |
|------------|---------------------------------------|
| feat       | Ajoute une nouvelle fonctionnalité (**MINOR**) |
| fix        | Corrige un bug (**PATCH**)            |
| chore      | Changements aux outils auxiliaires (**none**) |
| docs       | Modifications de documentation (**none**) |
| style      | Changements de style/code (**none**)  |
| refactor   | Refactorisation sans nouvelle feature ou bugfix (**none**) |
| perf       | Améliorations de performance (**PATCH**) |
| test       | Ajout/mise à jour de tests (**none**) |
| ci         | Changements de configuration CI (**none**) |

---

### 3. **Exemples de messages de commit**

- **Ajout de fonctionnalité :**
  ```text
  feat(auth): add JWT refresh token endpoint

  Adds a new authentication endpoint for refreshing JWTs.
  ```

- **Correction de bug :**
  ```text
  fix(router): handle trailing slash in URL parsing

  Corrects bug where router dropped trailing slashes.
  ```

- **Changement majeur :**
  ```text
  refactor(core)!: remove deprecated event emitter API

  BREAKING CHANGE: `EventEmitter` now requires explicit subscription.
  ```

