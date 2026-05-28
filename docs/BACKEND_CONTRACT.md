# Backend Contract

The plugin talks to a merchant backend relay. The relay talks to MakePay and verifies webhooks.

## Create Checkout

`POST /minecraft/checkout`

```json
{
  "serverId": "survival-1",
  "playerUuid": "00000000-0000-0000-0000-000000000000",
  "playerName": "Alex",
  "packageId": "vip",
  "displayName": "VIP Rank",
  "amountMinor": 999,
  "currency": "USD",
  "description": "VIP rank for this server"
}
```

Response:

```json
{
  "checkoutUrl": "https://checkout.makepay.io/pay/example",
  "externalId": "mc_survival-1_order_123",
  "expiresAt": "2026-05-29T12:00:00Z"
}
```

## Poll Entitlements

`GET /minecraft/entitlements?serverId=survival-1&cursor=abc`

Response:

```json
{
  "nextCursor": "def",
  "entitlements": [
    {
      "id": "ent_123",
      "externalId": "mc_survival-1_order_123",
      "playerUuid": "00000000-0000-0000-0000-000000000000",
      "playerName": "Alex",
      "packageId": "vip",
      "status": "paid",
      "commands": [
        "lp user {player} parent add vip"
      ]
    }
  ]
}
```

If `commands` is omitted or empty, the plugin uses package commands from `config.yml`.

## Acknowledge Entitlement

`POST /minecraft/entitlements/{id}/ack`

```json
{
  "serverId": "survival-1",
  "entitlementId": "ent_123",
  "acknowledgedAt": "2026-05-29T12:00:00Z"
}
```

Acknowledgement should be idempotent.
