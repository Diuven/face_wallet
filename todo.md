* Attach DB
  * First, make skeleton entities with only h2
  * Then add postgres setup
  * Wallet table, Transaction table
* Error handling
    * HTTP errors (codes & messages) & responses
    * proper logging
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
* Response DTOs
  * Gather and make structures of each data classes
  * DB models / Responses / Request bodies / Exceptions & Errors
* Namings
  * Project name (change from docker_demo!)
  * Package name
* Update web3j to recent version
  * Do not use web3j-spring-boot-starter (outdated & security issues)