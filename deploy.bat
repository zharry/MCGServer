scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/MinecraftGamesServer.jar
ssh -i C:\Users\Harry\.ssh\jackypc -p 25510   mcg@home.jackyliao.me "cd /home/mcg/Servers; cp MinecraftGamesServer.jar Lobby/plugins; cp MinecraftGamesServer.jar Parkour/plugins; cp MinecraftGamesServer.jar Spleef/plugins; cp MinecraftGamesServer.jar Dodgeball/plugins; cp MinecraftGamesServer.jar SurvivalGames/plugins"

::ssh -i C:\Users\Harry\.ssh\jackypc -p 25510 mcg@home.jackyliao.me "cd /home/mcg/Servers; cp MinecraftGamesServer.jar Lobby/plugins;                                              cp MinecraftGamesServer.jar Spleef/plugins; cp MinecraftGamesServer.jar Dodgeball/plugins; cp MinecraftGamesServer.jar SurvivalGames/plugins"

::scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/Lobby/plugins/MinecraftGamesServer.jar
::scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/Parkour/plugins/MinecraftGamesServer.jar
::scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/Spleef/plugins/MinecraftGamesServer.jar
::scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/Dodgeball/plugins/MinecraftGamesServer.jar
::scp -i C:\Users\Harry\.ssh\jackypc -P 25510 D:\OneDrive\Programming\IdeaProjects\MinecraftGamesServer\builds\MinecraftGamesServer.jar mcg@home.jackyliao.me:/home/mcg/Servers/SurvivalGames/plugins/MinecraftGamesServer.jar
exit