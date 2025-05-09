# phileas-connector
Phileas functions for Trino

This [Trino](https://trino.io) connector uses [Phileas](https://github.com/philterd/phileas) to detect and redact PII tokens.
Simple scalar functions are provided to redact `varchar` data provided by any other Trino data source.

[![CodeFactor](https://www.codefactor.io/repository/github/philterd/phileas-connector/badge)](https://www.codefactor.io/repository/github/philterd/phileas-connector)

## Dependencies

* Java 23
* Maven 3.9.x
* [philterd/phileas](https://github.com/philterd/phileas) 
* Trino (see `pom.xml` for version)

## Configuring Trino

```
1. Install Trino
download and expand tarball to local directory
create etc directory as described here: https://trino.io/docs/current/installation/deployment.html
export TRINO_HOME=$HOME/...

2. Create $TRINO_HOME/etc/catalog/phileas.properties:
connector.name=phileas
phileas.policy.file=/Users/me/policy.txt

3. Build the connector and redeploy
mvn clean package && rm -rf $TRINO_HOME/plugin/phileas && cp -r ./target/phileas-connector-475 $TRINO_HOME/plugin/phileas

4. Start Trino
cd $TRINO_HOME
bash bin/launcher run
```

## Redacting Strings

Use DBeaver or your favorite SQL editor to try the `phileas_redact` function.

```
select phileas_redact('my email is rik@resurfacd.io')
--> my email is ****************
```

---
Copyright 2024-2025 Philterd, LLC @ https://www.philterd.ai