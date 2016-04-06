fastlane documentation
================
# Installation
```
sudo gem install fastlane
```
# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android beta
```
fastlane android beta
```
Submit a new Beta Build to Crashlytics Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play. export SUPPLY_TRACK=beta to deploy to Google Beta

need to set your Google signing key credentails in envrionmemnt variables KEYSTORE, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD
### android release
```
fastlane android release
```
make a release. This lane requires: 1) developers should have already added release notes in README.md and modified version number strings in various files;

 2) there is already a release/x.y.z branch existing. Developers make a PR from release branch to master branch, release engineer merges PR, than run command like

 fastlane release version_number:"1.3.00"

----

This README.md is auto-generated and will be re-generated every time to run [fastlane](https://fastlane.tools).
More information about fastlane can be found on [https://fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [GitHub](https://github.com/fastlane/fastlane/tree/master/fastlane).