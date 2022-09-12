## Face wallet

### Setup

`./docker-compose.yml` includes setup for api and database container. `Dockerfile` includes configuration for api container.

#### Prerequisites
* Docker and docker compose installed
  * Docker desktop is optional, but recommended
* Unix environment

#### Build
* Run `docker compose up -d` to run

#### Development
* Currently, the change of the code is not applied to the running server. Please restart the api container to see the change.
  * Api container can be restarted by `docker restart <container_id>`
* To inspect the database, consider using external tools such as database tool in IntelliJ or TablePlus.
* Api server listens in port 8080. You can test apis with curl or any http client such as web browser, postman, or Intellij plugin.


### Structure

* There are three parts of data layers: Ethereum nodes, Postgres DB, and user's client.
  * Ethereum node is running on external instance, and will hold the true updated data of the blockchain.
  * Postgres DB is running on the server, which will maintain the partial and staged data of accounts managed by api.
  * User's client will only take care of own private key and necessary account addresses.
    * If necessary, user can export their account or wallet by using their private key and mnemonic.
* The api can also be separated to three layers: Controllers, Services, and Repositories.
  * Controllers should handle the most basic and necessary logics, such as validation and IOs to the user.
  * Repositories are solely purposed to update the database. Along with the entities, it defines how the database should work.
  * Services take the majority of non-trivial business logics like organizing data fetched from repositories and web3 to serve the request received from the controller.
* Errors received by users can be categorized as following:
  * Bad request errors, which can be checked only using db (i.e. not checking web3)
    * Invalid parameters, duplicate entries, etc.
  * Web3 node connection errors
    * Node disconnected or timeout
  * Errors received from web3 node
    * Transaction errors which might have caused because of outdated database, or external activities
* Logs will be printed on following events:
  * Before and after request of any api endpoints
  * On any sql query to database
  * On any request sent to web3 node
  * On any 5xx errors sent to user (with stacktrace) 
* Testing

####
