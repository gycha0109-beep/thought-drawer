# Android Release Signing

This project is configured to sign `release` builds from either:

1. A local `keystore.properties` file in the project root
2. Environment variables

Secrets are not stored in source control.

## What You Need To Create

Create or provide these four values:

- `release.storeFile`: the path to your keystore file, for example `release-keystore/brainclean-upload.jks`
- `release.storePassword`: the password that unlocks the keystore file
- `release.keyAlias`: the alias of the key inside that keystore, for example `upload`
- `release.keyPassword`: the password for that alias

You also need the actual keystore file itself:

- Recommended file name: `brainclean-upload.jks`
- Recommended folder: `release-keystore/`

## Recommended Keystore Strategy

If you plan to publish on Google Play, use an upload keystore:

- Keep this keystore private and backed up in at least two safe places
- Do not commit it to Git
- If you enroll in Play App Signing, Google manages the app signing key and you keep only the upload key

## Create A New Keystore

From the project root, run:

```powershell
New-Item -ItemType Directory -Force release-keystore
keytool -genkeypair `
  -v `
  -keystore release-keystore/brainclean-upload.jks `
  -alias upload `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

`keytool` will ask for:

- Keystore password
- First and last name
- Organizational unit
- Organization
- City or locality
- State or province
- Two-letter country code
- Key password

For team-owned apps, use your company or team identity consistently.

## Create `keystore.properties`

Copy [keystore.properties.example](keystore.properties.example) to `keystore.properties` and fill it in:

```properties
release.storeFile=release-keystore/brainclean-upload.jks
release.storePassword=YOUR_KEYSTORE_PASSWORD
release.keyAlias=upload
release.keyPassword=YOUR_KEY_PASSWORD
```

## Environment Variable Alternative

Instead of `keystore.properties`, you can set:

```powershell
$env:BRAINCLEAN_RELEASE_STORE_FILE="release-keystore/brainclean-upload.jks"
$env:BRAINCLEAN_RELEASE_STORE_PASSWORD="YOUR_KEYSTORE_PASSWORD"
$env:BRAINCLEAN_RELEASE_KEY_ALIAS="upload"
$env:BRAINCLEAN_RELEASE_KEY_PASSWORD="YOUR_KEY_PASSWORD"
```

## Build Signed Release Artifacts

Signed APK:

```powershell
cmd /c gradlew.bat --no-daemon :app:assembleRelease
```

Signed AAB:

```powershell
cmd /c gradlew.bat --no-daemon :app:bundleRelease
```

Outputs:

- `app/build/outputs/apk/release/`
- `app/build/outputs/bundle/release/`

## Failure Behavior

If signing is not configured, release tasks fail with a clear error message instead of producing an accidental unsigned release.

## Important Safety Notes

- Back up the keystore and passwords before shipping
- Losing the keystore can block future updates outside Play App Signing recovery flows
- Never store passwords in committed files
- Never rename or replace the keystore casually after a release is published
