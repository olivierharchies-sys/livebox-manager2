# Livebox Manager 📱

Application Android qui permet de paramétrer **visuellement** un modem-routeur **Orange Livebox** depuis son téléphone (Android 8+).

## Fonctionnalités

- **Connexion sécurisée** à la Livebox via son API locale (`http://192.168.1.1/ws`) avec le mot de passe admin.
- **Tableau de bord** : état Internet, modèle, firmware, liste des appareils connectés (Wi-Fi, Ethernet, CPL).
- **WiFi** : renommer les réseaux 2,4 GHz / 5 GHz, changer les canaux avec des sliders.
- **Contrôle parental** : couper l'accès Internet d'un appareil et planifier des horaires.
- **Pare-feu** : niveau de protection, UPnP, blocage des pings, pare-feu IPv6.

## Technologie

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- OkHttp pour l'API `sysbus` de la Livebox
- Thème clair/sombre aux couleurs Orange

## Ouvrir le projet

1. Ouvrir le dossier dans **Android Studio** (Koala ou plus récent, JDK 17).
2. Laisser Gradle synchroniser (si le wrapper Gradle manque, Android Studio le régénère automatiquement).
3. Connecter le téléphone sur le **même Wi-Fi que la Livebox** et lancer l'app.

## Notes

⚠️ L'appareil doit être sur le réseau local de la Livebox. Les structures exactes de l'API
`sysbus` varient selon les firmwares (Livebox 4/5/6) : les fonctions de `LiveboxApi.kt`
peuvent nécessiter un ajustement mineur selon votre modèle. Le mot de passe admin est celui
de l'interface web de la Livebox (étiquette ou mot de passe personnalisé).

## Licence

MIT
