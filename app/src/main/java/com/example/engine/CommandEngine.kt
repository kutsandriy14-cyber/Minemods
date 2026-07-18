package com.example.engine

import com.example.game.BlockRegistry
import com.example.game.Mob
import com.example.game.MobType
import com.example.game.Player
import com.example.world.World

object CommandEngine {
    
    // Executes a text command and returns a response message
    fun executeCommand(commandText: String, player: Player, world: World): String {
        val clean = commandText.trim()
        if (!clean.startsWith("/")) {
            return "Commands must start with '/'"
        }
        
        val parts = clean.substring(1).split(" ").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "Unknown command"
        
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)
        
        return when (cmd) {
            "gamemode", "gm" -> {
                if (args.isEmpty()) return "Usage: /gamemode <survival|creative|spectator>"
                val modeStr = args[0].lowercase()
                when (modeStr) {
                    "survival", "s", "0" -> {
                        player.gameMode = Player.GameMode.SURVIVAL
                        "Set game mode to Survival Mode"
                    }
                    "creative", "c", "1" -> {
                        player.gameMode = Player.GameMode.CREATIVE
                        "Set game mode to Creative Mode"
                    }
                    "spectator", "sp", "2" -> {
                        player.gameMode = Player.GameMode.SPECTATOR
                        "Set game mode to Spectator Mode"
                    }
                    else -> "Unknown game mode: $modeStr"
                }
            }
            "fly" -> {
                if (args.isEmpty()) {
                    player.canFly = !player.canFly
                    if (!player.canFly) player.isFlying = false
                    return "Flight toggled to: ${player.canFly}"
                }
                val flyOn = args[0].lowercase() == "on" || args[0].lowercase() == "true" || args[0] == "1"
                player.canFly = flyOn
                if (!flyOn) player.isFlying = false
                "Set flight ability to: $flyOn"
            }
            "time" -> {
                if (args.size < 2 || args[0].lowercase() != "set") {
                    return "Usage: /time set <day|night>"
                }
                val timeStr = args[1].lowercase()
                when (timeStr) {
                    "day" -> {
                        world.isNight = false
                        "Set time to Day (0)"
                    }
                    "night" -> {
                        world.isNight = true
                        "Set time to Night (18000)"
                    }
                    else -> "Unknown time: $timeStr"
                }
            }
            "heal" -> {
                player.health = player.maxHealth
                player.goldHearts = 4
                "Healed completely and added golden absorption hearts!"
            }
            "give" -> {
                if (args.isEmpty()) return "Usage: /give <block_name_or_id>"
                val blockQuery = args[0].lowercase()
                var foundBlock: Byte? = null
                
                // Find block by ID or Name
                for (b in 1..120) {
                    val bType = b.toByte()
                    val bName = BlockRegistry.getBlockName(bType).lowercase().replace(" ", "_")
                    if (bName.contains(blockQuery) || bType.toString() == blockQuery) {
                        foundBlock = bType
                        break
                    }
                }
                
                if (foundBlock != null) {
                    // Put in inventory
                    var added = false
                    for (i in 0 until player.inventory.size) {
                        if (player.inventory.items[i] == BlockRegistry.AIR || player.inventory.items[i] == foundBlock) {
                            player.inventory.items[i] = foundBlock
                            added = true
                            break
                        }
                    }
                    if (added) {
                        "Gave 1x ${BlockRegistry.getBlockName(foundBlock)}"
                    } else {
                        "Inventory full!"
                    }
                } else {
                    "Block or item not found matching '$blockQuery'"
                }
            }
            "spawn" -> {
                if (args.isEmpty()) return "Usage: /spawn <zombie|pig>"
                val mobStr = args[0].lowercase()
                val type = when (mobStr) {
                    "zombie", "z" -> MobType.ZOMBIE
                    "pig", "p" -> MobType.PIG
                    else -> return "Unknown mob type. Use: zombie, pig"
                }
                
                val px = player.camera.position.x
                val py = player.camera.position.y
                val pz = player.camera.position.z
                
                // Spawn 2 blocks in front of player
                val dx = player.camera.front.x * 2.5f
                val dz = player.camera.front.z * 2.5f
                
                val mob = Mob(type, world, px + dx, py + 1f, pz + dz)
                world.mobs.add(mob)
                "Spawned ${type.name} in front of player!"
            }
            "op" -> {
                player.isOp = true
                "Granted operator privileges"
            }
            "deop" -> {
                player.isOp = false
                "Revoked operator privileges"
            }
            "help", "?" -> {
                "Available commands: /gamemode, /fly, /time, /heal, /give, /spawn, /op, /deop"
            }
            else -> "Unknown command. Type /help for assistance."
        }
    }
}
