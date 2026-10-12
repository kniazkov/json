<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Publish a release to Maven Central

[Guide](README.md) · [Development](development.md)

The `release` Maven profile signs the artifacts with GPG and enables the Central
Publishing Maven Plugin. Normal builds and pull-request checks need neither a
signing key nor Central credentials.

## Prepare the publisher account

Use a [Central Portal](https://central.sonatype.com/) account authorized to publish
in the `com.kniazkov` namespace. Generate a
[Portal user token](https://central.sonatype.org/publish/generate-portal-token/)
and add its two values to the `central` server in your local `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>${env.CENTRAL_USERNAME}</username>
            <password>${env.CENTRAL_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

Merge this entry into your existing settings if you already have them. These are
the token credentials supplied through `CENTRAL_USERNAME` and `CENTRAL_PASSWORD`,
not the password used to sign into the website. Keep this
file and your private signing key outside the repository.

Install GnuPG, import your release signing key, and make its public key available
as described in the [Central GPG guide](https://central.sonatype.org/publish/requirements/gpg/).
For an interactive terminal, configure pinentry before running Maven:

```sh
export GPG_TTY=$(tty)
gpg --list-secret-keys --keyid-format LONG
```

Select a particular key with `-Dgpg.keyname=YOUR_KEY_FINGERPRINT` if necessary.
GPG obtains its passphrase through its agent/pinentry; do not put the passphrase
in a command line or commit it in the POM.

## Check the release

Confirm that the version in `pom.xml` is the intended, unpublished release version
(`2.0.0` for this release). Central release coordinates cannot be overwritten.
Run the checks from a clean checkout using Java 21+ and Maven 3.6.3+:

```sh
mvn -B -ntp -DargLine=-Xss256k clean verify
python3 scripts/check-doc-examples.py
```

## Check signing without uploading

```sh
mvn -Prelease clean verify
```

This runs the build checks, creates the binary, sources and Javadoc JARs, and
signs those three JARs and the POM. Inspect the four `.asc` signatures alongside
their files in `target`. No Central credentials are needed: `verify` stops before
the upload phase. Do not set `gpg.skip` for a release.

```sh
gpg --verify target/json-2.0.0.jar.asc target/json-2.0.0.jar
```

Use `verify` for this local check, not `deploy` with `skipPublishing`: in plugin
0.11.0 that flag also skips staging the artifacts and does not produce a bundle.

## Upload and confirm publication

```sh
mvn -Prelease clean deploy
```

This builds and signs the release, uploads the bundle using the `central` server
credentials, and waits for Portal validation. It does **not** automatically make
the release public: `autoPublish` is explicitly disabled.

Open [your deployments](https://central.sonatype.com/publishing/deployments), check
the validated component and version, and confirm **Publish**. If validation fails,
inspect its report, correct the problem, and upload a new bundle. A successful
local build alone does not prove that the account has namespace permissions or
that Central can retrieve the signing key.

There is no publication on push, pull request, or tag creation. GitHub Actions
runs the ordinary checks without publisher credentials. Uploading a release is an
explicit maintainer action.

After publication, tag the exact commit that passed CI as `v2.0.0`, create a
GitHub release, and check that `com.kniazkov:json:2.0.0` resolves in a clean consumer
project. For subsequent releases, update the POM version, SCM tag and documentation
examples together before building.

For the plugin's configuration and Portal behavior, see the
[official Maven publishing guide](https://central.sonatype.org/publish/publish-portal-maven/).
