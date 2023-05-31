# Visual Key API
How the server is built and run see `Dockerfile` and `.github/workflows/main.yml` files.

## Supported environment variables:
- DEVELOPMENT_MODE - Is the server in development mode? Optional. Defaults to false
- HTTP_HOST - Hostname of the server. Optional. Defaults to 0.0.0.0
- HTTP_PORT - Port of the server. Optional. Defaults to 8080
- ENVIRONMENT - Environment of the server. Optional. Defaults to local. Values: local, prod
- BEHIND_PROXY - Is the server behind a proxy? Optional. Defaults to false
- APP_SIGNER - 256-bit key to sign price requests
- SECRETS_FILE_PATH - Path to the secrets file. If specified, adds -config=$SECRETS_FILE_PATH to the program arguments
- SECRETS_FILE_DATA - Base64 encoded secrets file data. If specified, writes the data to a file and adds -config=$SECRETS_FILE_PATH to the program arguments

The app requires a secrets.conf file that can be passed to the app using SECRETS_FILE_PATH, SECRETS_FILE_DATA environment variables, or manually through the -config ktor parameter.

Configuration sample

```conf
app {
  signerPrivateKey = "0x0000000000000000000000000000000000000000000000000000000000000001"
}

chains = [
  {
    chainId = 11155111
    region = "testnet"
    name = "Sepolia"
    currency = "ETH"
    apis = ["https://rpc.example.com"]
  }
  {
    chainId = 80001
    region = "testnet"
    name = "Polygon Mumbai"
    currency = "MATIC"
    apis = ["https://rpc.example.com"]
  }
  {
    chainId = 31337
    region = "local"
    name = "Ethereum"
    currency = "ETH"
    apis = ["http://localhost:8545"]
  }
]

nfts = [
  {
    chainId = 11155111
    contracts = [
      {
        address = "0x0000000000000000000000000000000000000000"
        signerPrivateKey = "0x0000000000000000000000000000000000000000000000000000000000000001"
      }
    ]
  }
  {
    chainId = 80001
    contracts = [
      {
        address = "0x0000000000000000000000000000000000000000"
        signerPrivateKey = "0x0000000000000000000000000000000000000000000000000000000000000001"
      }
    ]
  }
]

databases {
  visualKeyCosmosDb {
    uri = "https://[dbname].documents.azure.com"
    key = "XXXXXXXXX"
  }
}
```
