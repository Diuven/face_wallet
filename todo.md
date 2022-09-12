* APIs
  * PollingTransactionReceiptProcessor
    * Create transaction, and give hash and checking url
    * Follow the transaction status on that other url
* Attach DB
  * Then add postgres setup
  * Indexing (on txs)
  * External / Internal wallets
* Testing
    * Unit tests
    * Demo interface (HTML or React SPA)
* Crypto protocols
    * Provide full access of their wallets to users (i.e. can be reconstructed in other services if desired)
    * Server should not be able to take control of the wallet
    * Avoid private keys or such important data to be transferred upon request
      * Maybe we can use salt? or shared key?
* Add better logging
    * Customized request & response logging
      * sensitive information should not be logged
    * Consistent logging format between http context logging and web3 logging
    * Save in files
* Cron
  * To periodically fetch blocks & update db
  * Validate db tables
  * Subscribe to internal nodes (just to be aware or external txs)
* Docs & Readme
  * RestDocs
  * Structures
  * How to run & develop & investigate
* DTOs
  * Configs (Endpoint url, block confirmation threshold, etc)
* Namings
  * Project name (change from docker_demo!)
  * Package name
* Update web3j to recent version
  * Do not use web3j-spring-boot-starter (outdated & security issues)