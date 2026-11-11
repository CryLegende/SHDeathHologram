<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHDeathHologram

*Affiche un hologramme temporaire à l'endroit où un joueur meurt, avec la victime et le tueur.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale%20%C2%B7%20Lobby-3aa655?style=for-the-badge" alt="SkyHand Opale · Lobby">

</div>

---

## Présentation

Affiche un hologramme temporaire à l'endroit où un joueur meurt, avec la victime et le tueur.

Utilisé sur : **Opale · Lobby**.

## Dépendances

- Obligatoires : `FancyHolograms`
- Optionnelles : `PlaceholderAPI`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/shdeathhologram reload` | — | Relit `config.yml` et `messages.yml` (la console peut l'utiliser). Appliqué aux prochaines morts ; en cas d'erreur YAML, l'ancienne configuration reste en place et le fichier fautif est indiqué. | `shdeathhologram.admin.reload` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shdeathhologram.admin` | Toutes les sous-commandes de `/shdeathhologram`. | `op` |
| `shdeathhologram.admin.reload` | `/shdeathhologram reload`. | `op` |

## Configuration

Fichiers par défaut repris du serveur ; `messages.yml` est créé au premier démarrage s'il manque. Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHDeathHologram/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
