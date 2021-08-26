#!/bin/bash

#rm /home/mcg/ResourcePack/test.zip
#cd resourcepack
#zip -r /home/mcg/ResourcePack/test.zip *
#sha256sum /home/mcg/ResourcePack/test.zip
#
#cd ../
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/Dodgeball/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/Lobby/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/Spleef/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/SurvivalGames/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/Parkour/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/ElytraRun/plugins
cp out/artifacts/MinecraftGamesServer.jar/MinecraftGamesServer.jar /home/mcg/Servers/Proxy/plugins
