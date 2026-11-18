# Coumpanion Server

Coumpanion Server runs DeepSeek chats. You can set it up from your phone using a cloud server's browser console or an Android SSH app. The app connects over mobile data or Wi-Fi through your Cloudflare relay. A local Wi-Fi connection also works when the server is on your network.

## From your phone

Open the console for a Linux cloud server you control. Use Bash with Node.js 22 or later, npm, OpenSSL, and the updated Coumpanion source. You'll also need a Cloudflare account with a `workers.dev` subdomain and a DeepSeek API key. Run the commands from the root of the source checkout.

### 1. Set up the relay

In the Cloudflare dashboard, copy your account ID and create a token using the **Edit Cloudflare Workers** template, scoped to your account. Enter them in the console when prompted. Bash hides the token as you paste it.

```bash
cd android/relay
npm ci
read -rp 'Cloudflare account ID: ' CLOUDFLARE_ACCOUNT_ID
read -rsp 'Cloudflare API token: ' CLOUDFLARE_API_TOKEN
printf '\n'
export CLOUDFLARE_ACCOUNT_ID CLOUDFLARE_API_TOKEN WRANGLER_SEND_METRICS=false
npm exec -- wrangler deploy
```

Save the Worker address shown by Wrangler. Make a private access key and set it on the Worker.

```bash
export COUMPANION_RELAY_ACCESS_KEY=$(openssl rand -base64 32 | tr '+/' '-_' | tr -d '=')
printf '%s' "$COUMPANION_RELAY_ACCESS_KEY" | npm exec -- wrangler secret put ACCESS_KEY
unset CLOUDFLARE_API_TOKEN
```

### 2. Start the server and copy its link

Enter the Worker address with `wss://` in place of `https://`. Enter your DeepSeek key at the hidden prompt. The server runs on the cloud host and connects out to the relay. You can close the console after it starts.

```bash
cd ../server
npm ci
read -rp 'Worker address starting with wss://: ' COUMPANION_RELAY_URL
read -rsp 'DeepSeek API key: ' DEEPSEEK_API_KEY
printf '\n'
export COUMPANION_RELAY_URL DEEPSEEK_API_KEY
nohup node run.mjs > "$HOME/coumpanion-server.log" 2>&1 </dev/null &
unset DEEPSEEK_API_KEY COUMPANION_RELAY_ACCESS_KEY
tail "$HOME/coumpanion-server.log"
```

When the log shows **Relay connected. Waiting for the phone.**, run this in the same private console and copy the entire `coucou://pair?…` link.

```bash
cat ~/.local/state/coumpanion/pairing.txt
```

Keep the link private. If the host reboots, start the server again with the same Worker address, access key, DeepSeek key, and state directory to keep your pairing. Store the access key privately so you can re-enter it when starting the server again.

### 3. Pair and send a message

Open Coumpanion and tap **Paste the link** → **Paste from clipboard** → **Pair**. Open **Chat**, choose **DeepSeek Flash**, and send a message. Leave **Away from home Wi-Fi** enabled in Settings to use the relay. With **Display over other apps** enabled, the island can show progress while you use another app.

This pairing is for the copy of Coumpanion Server you started. It creates a new DeepSeek conversation in Coumpanion.

## Try the link

Install Node.js 22 or later and OpenSSL on the server. From the checkout, start the fake provider for a local test. Replace `192.168.1.20` with the server's private IPv4 address reachable from the phone.

```bash
cd android
node server/run.mjs --host 192.168.1.20 --fake
```

In a separate terminal on the same server, read the private pairing link.

```bash
cat ~/.local/state/coumpanion/pairing.txt
```

In Coumpanion, tap **Paste the link**, paste it, then tap **Pair**. Open **Chat**, pick **DeepSeek Flash (fake test)**, and send a message. The server emits Working and Finished events and sends a clearly labelled test answer in Chat. With **Display over other apps** on, switch to another app after sending a task to see those states in the island.

## Use DeepSeek

If you ran the fake server, stop it with **Ctrl+C**. Enter your own DeepSeek API key in the server shell. Bash's silent read keeps the key out of the command history and screen. Use the same address and state directory to keep an existing pairing.

```bash
cd android
read -rsp 'DeepSeek API key: ' DEEPSEEK_API_KEY
export DEEPSEEK_API_KEY
node server/run.mjs --host 192.168.1.20
```

Once the server starts, run this in another terminal on that server to get the private pairing link.

```bash
cat ~/.local/state/coumpanion/pairing.txt
```

Copy the full `coucou://pair?…` link. In the Android app, tap **Paste the link**, paste it, and tap **Pair**.

The model is `deepseek-flash` at `https://api.deepseek.com`. The key stays on the server. Open Chat and send a task. The server keeps the latest task status on disk so a restart can report an interruption. Conversation text stays in memory while the server is running, and the phone stores its own chat as usual. Switching apps can leave the link active. Losing the link mid-answer interrupts that chat request under the current protocol.

## Other assistants

This server runs fresh conversations against the DeepSeek API. ChatGPT, Codex, and Claude mobile app sessions aren't a pairing endpoint. Use a compatible Coucou desktop or CLI integration to view supported agents already running on another host. Adding a model provider to this server means configuring a provider on the server side. The phone uses the same Chat tab and never needs the provider key.

## What is ready

Direct connections use the existing v1 pairing and certificate pinning. A Linux server with Avahi can announce the address on the LAN. Keep the state folder private, including the pairing link. Use one server process per state folder.

The server supports provider chat and Working/Finished session progress. The relay passes the same encrypted link messages on Wi-Fi and mobile data. Approval requests come from compatible desktop integrations.

```bash
cd android/server
npm test
```
