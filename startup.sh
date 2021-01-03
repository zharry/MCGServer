#!/bin/bash

cd /home/harry/MCG

echo "Starting Proxy server..."
cd Proxy
tmux new-session -d -s "proxy" ./server.sh
tmux ls

sleep 10s

echo "Starting Lobby server..."
cd ../Lobby
tmux new-session -d -s "lobby" ./server.sh
tmux ls

sleep 30s

#echo "Starting Parkour server..."
#cd ../Parkour
#tmux new-session -d -s "parkour" ./server.sh
#tmux ls
#
#sleep 30s
#
#echo "Starting Spleef server..."
#cd ../Spleef
#tmux new-session -d -s "spleef" ./server.sh
#tmux ls
#
#sleep 30s
#
echo "Starting Dodgeball server..."
cd ../Dodgeball
tmux new-session -d -s "dodgeball" ./server.sh
tmux ls

sleep 30s
#
#echo "Starting Survival Games server..."
#cd ../SurvivalGames
#tmux new-session -d -s "survivalgames" ./server.sh
#tmux ls
#
#sleep 30s

echo "All done!"
echo "Use tmux ls to see all available consoles"
echo "Use tmux attach-session -t name to attach to a console"

while true; do sleep 30; done

#"C:\Program Files\Git\bin\bash.exe"