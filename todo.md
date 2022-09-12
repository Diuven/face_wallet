* ~~APIs~~
  * ~~PollingTransactionReceiptProcessor~~
    * ~~Create transaction, and give hash and checking url~~
    * ~~Follow the transaction status on that other url~~
* Attach DB
  * Indexing (on txs)
    * Should be better
  * ~~External / Internal wallets~~
* Testing
    * Unit tests & Integration tests
    * ~~Demo interface (HTML or React SPA)~~
* ~~Crypto protocols~~
    * ~~Provide full access of their wallets to users (i.e. can be reconstructed in other services if desired)~~
    * ~~Server should not be able to take control of the wallet~~
    * ~~Avoid private keys or such important data to be transferred upon request~~
      * ~~Maybe we can use salt? or shared key?~~
* Add better logging
    * Customized request & response logging
      * sensitive information should not be logged
    * ~~Save in files~~
* ~~Cron~~
  * ~~To periodically fetch blocks & update db~~
  * ~~Validate db tables~~
  * ~~Subscribe to internal nodes (just to be aware or external txs)~~
* Docs & Readme
  * RestDocs
  * Structures
  * How to run & develop & investigate
* Update web3j to recent version
  * Do not use web3j-spring-boot-starter (outdated & security issues)