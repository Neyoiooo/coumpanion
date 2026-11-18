# Coumpanion Server

Coumpanion Server hosts the AI connection. Run it from the shell of a machine you control. You can use a phone's SSH app to reach that shell. The phone connects to the server over private Wi-Fi or a private network, with its own pairing link.

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

Stop the fake server with **Ctrl+C**. Enter your own DeepSeek API key in the server shell. Bash's silent read keeps the key out of the command history and screen. Use the same address and state directory to keep the pairing.

```bash
read -rsp 'DeepSeek API key: ' DEEPSEEK_API_KEY
export DEEPSEEK_API_KEY
node server/run.mjs --host 192.168.1.20
```

The model is `deepseek-flash` at `https://api.deepseek.com`. The key stays on the server. Open Chat and send a task. The server keeps the latest task status on disk so a restart can report an interruption. Conversation text stays in memory while the server is running, and the phone stores its own chat as usual. Switching apps can leave the link active. Losing the link mid-answer interrupts that chat request under the current protocol.

## Other assistants

This server runs fresh conversations against the DeepSeek API. ChatGPT, Codex, and Claude mobile app sessions aren't a pairing endpoint. Use a compatible Coucou desktop or CLI integration to view supported agents already running on another host. Adding a model provider to this server means configuring a provider on the server side. The phone uses the same Chat tab and never needs the provider key.

## What is ready

Direct connections use the existing v1 pairing and certificate pinning. A Linux server with Avahi can announce the address on the LAN. Keep the state folder private, including the pairing link. Use one server process per state folder.

This step supports provider chat and Working/Finished session progress. Server approvals and mobile-data relay testing are the next step. The existing desktop integrations can still send their approval requests to the phone.

```bash
node --test server/provider.test.mjs
```
