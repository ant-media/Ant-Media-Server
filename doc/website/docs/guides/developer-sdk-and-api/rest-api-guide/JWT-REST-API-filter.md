# JWT REST API filter

In this section, we're going to explain simply how to use ```JWT REST API Filter``` in Ant Media Server. By default, ```JWT REST API Filter``` is disabled and ```REST API IP Filter``` is enabled. You can use JWT Filter when you consume REST API from different endpoints. Before starting, you can get more information about JWT on [jwt.io](https://jwt.io/). 

Here is a simple step-by-step guide for using JWT REST API Filter. Note that this feature is available in Ant Media Server 2.4 and above.

## Enable JWT filter

Ant Media Server uses [JJWT library](https://github.com/jwtk/jjwt) for REST API security. If you want to enable this filter, you just need to enable JWT REST API Filter and type the Secret key on the web panel. Secret key encrypts with ```HMAC-SHA256``` in JWT REST API Filter.

![](@site/static/img/jwt-filter-enable.png)

## Generate a JWT token

Let's assume that our secret key is ```zautXStXM9iW3aD3FuyPH0TdK4GHPmHq``` and we need to create a JWT token. Luckily, there are plenty of libraries available at [Libraries for JWT](https://jwt.io/#libraries-io) for your development. For our case, we will just use [Debugger at JWT](https://jwt.io/#debugger-io)

![](@site/static/img/generate_jwt_token.png)

As shown above, we use ```HS256``` an algorithm and use our secret key ```zautXStXM9iW3aD3FuyPH0TdK4GHPmHq``` to generate the token. Our JWT token to access the REST API is:

eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0b2tlbiIsImlhdCI6MTUxNjIzOTAyMn0.OESIxgNsnD_JwByKTXcrw9Ov4GaOUZw66QxMfmudhKQ
### Generate JWT token with expiration time

Even if it's not necessary to have the payload, there are really useful options that can be used in JWT. For instance, you can use **exp**(expiration time) for JWT token. In order to get more information on the structure, please visit [Introduction to JWT](https://jwt.io/introduction). Here is an example about JWT Token with Expiration Time.

![](@site/static/img/generate-jwt-expire-time.png)

As shown above, the expiration time of the token is Mar 20, 2021 14:17:02 GMT+3. It means that you can use the generated token until the expiration time. The unit of expiration time is [unix timestamp](https://www.unixtimestamp.com/). When it expires, the JWT token becomes invalid.

### Use Token for Accessing REST Filter API

Using JWT token is so simple. Just add ```Authorization``` header with JWT Token as shown below.

curl -X POST -H "Content-Type: application/json" -H "Authorization: {JWTToken}" "https://{domain:port}/{application}/rest/v2/broadcasts/create" -d '{"name":"{streamName}"}'
You can also use Postman as follows:

![](@site/static/img/use_jwt_token_with_postman.png)

Note that this feature is available in Ant Media Server 2.3+ versions.

## Enable JWKS

The JSON Web Key Set (JWKS) is a set of keys containing the public keys used to verify any JSON Web Token (JWT) issued by the authorization server and signed using the RS256 signing algorithm.

If you want to use the JWKS feature, you need to have an OAuth server like auth0.com. You can build your own OAuth server system with Hydra. Read  [this document](https://www.ory.sh/hydra/docs/install) for installation.

Add JWKS configurations on app settings. Add parameters to ```/usr/local/antmedia/webapps/<App-Name>/WEB-INF/red5-web.properties``` file as below.

```
settings.jwtControlEnabled=true
settings.jwksURL=JWKS URL
```
 For example:

```
settings.jwtControlEnabled=true
settings.jwksURL=https://antmedia.us.auth0.com
```
Ant Media Server using JWKS needs to have the public keys used by the signing party to validate signatures. The anatomy of a JWKS is something like this: [https://antmedia.us.auth0.com/.well-known/jwks.json](https://antmedia.us.auth0.com/.well-known/jwks.json)

```
{
  "keys": [
    {
      "use": "sig",
      "kty": "RSA",
      "kid": "public:c424b67b-fe28-45d7-b015-f79da50b5b21",
      "alg": "RS256",
      "n": "sttddbg-_yjXzcFpbMJB1fIFam9lQBeXWbTqzJwbuFbspHMsRowa8FaPw44l2C9Q42J3AdQD8CcNj2z7byCTSC5gaDAY30xvZoi5WDWkSjHblMPBUT2cDtw9bIZ6FocRp46KaKzeoVDv3a0EBg5cdAdrefawfZoruPZCLmyLqXZmBM8RbpYLChb-UFO25i7e4AoRJ2hNFYg0qM-hRZNwLliDfkafjnOgSu7_w0WDInNzbUuy26rb_yDNGEIylXHlt0BKcMoeO3sJEwS5EDAkXkvz_7zQ6lgDQ4OLihC4QDwkp7dV2iQxvd7D-XEaSIahiqdHlqR8cUYOJANDVRIufAzzkyK8Shu_MXhVUW7hH3hNjlEh198bCWANHcsZWF2_V78Rl-UzCjsAFWtttf6FYpR9Kt-8ILM3aAYTAk3OwsvzSeqTtWLHp96QE8Bcm1AmZfPWzsd3PpLuSM_wfx4oxDWhdaKQ-HK1hCYLNv2Vity2uNC_tbGxOD9syRujWKS6wFf2b3jFEudV0NUXQ_1Beu8Ir0jHzuA_0D22wgiaSJ9svfpJ7XyoD6fxyHSyhpMsXIDLmnwOPKmD67MFQ7Bv_9H91KZmr34oeh6PVWEwb4wUAkDaCebo6h0gdMoDfZTq9Gn5S-Aq0-_-fIfyN9qrrQ0E1Q_QDhvqXx8eQ1r9smM",
      "e": "AQAB"
    },
    {
      "use": "sig",
      "kty": "RSA",
      "kid": "public:9b9d0b47-b9ed-4ba6-9180-52fc5b161a3a",
      "alg": "RS256",
      "n": "6f4qEUPMmYAyAQnGQOIx1UkIEVPPt1BnhDH70w3Gq6uYpm4hUyRFiM1oZ4_xB28gTmpR_SJZL31E_yZTLKPwKKsCDyF6YGhFtcyifhsLJc45GW4G4poX8Y34EIYlT63G9vutwNwzistWZZqBm52e-bdUQ7zjmWUGpgkq1GQJZyPz2lvA2bThRqqj94w1hqHSCXuAc90cN-Th0Ss1QhKesud7dIgaJQngjWWXdlPBqNYe1oCI04E3gcWdYRFhKey1lkO0WG4VtQxcMADgCrhFVgicpdYyNVqim7Tf31Is_bcQcbFdmumwxWewT-dC6ur3UAv1A97L567QCwlGDP5DAvH35NmL3w291tUd4q5Vlwz6gsRKqDhUSonISboWvvY2x_ndH1oE2hXYin4WL3SyCyp-De8d59C5UhC8KPTvA-3h_UfcPvz6DRDdNrKyRdKmn9vQQpTP9jMtK7Tks8qKxK4D4pesUmjiNMsVCo8AwJ-9hMd7TXamE9CErfDR7jCQONUMetLnitiM7nazCPXkO5tAhJKzQm1o0HvCVptwaa7MksfViK5YPMcCYc9bD1Uujo-782MXqAzdncu0nGKaJXnIsYB0-tFNiNXjuYFQ8KV5k5-Wnn0kga4CkCHlMU2umR19zFsFwFBdVngOYkCEG46KAgdGDqtj8t4d0GY8tcM",
      "e": "AQAB"
    }
  ]
}
```
Restart Ant Media Server.

```
sudo service antmedia restart
```
After applying these configurations, you can use the JWKS feature in your structure the same way with JWT Filter.
