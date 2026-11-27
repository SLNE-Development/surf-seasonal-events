@file:Suppress("UnstableApiUsage")

package dev.slne.surf.seasonalevents.paper.npc

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.DialogActionButtonBuilder
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.seasonalevents.api.progress.NpcReward
import dev.slne.surf.seasonalevents.api.progress.NpcStatus
import dev.slne.surf.seasonalevents.api.progress.NpcToday
import dev.slne.surf.seasonalevents.paper.plugin
import io.papermc.paper.dialog.Dialog
import net.kyori.adventure.text.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * What the event NPC says, as dialogs titled with the NPC's name. Which one is shown depends on the date
 * and the player's progress. Built per show, because their click callbacks work once.
 */
internal object NpcDialogs {

    private const val WIDTH = 300

    private val shortDay = DateTimeFormatter.ofPattern("dd.MM.")

    /** First talk: the NPC lost its gifts and asks for help; the button continues with [progress]. */
    fun intro(speaker: Component, status: NpcStatus.Active, today: LocalDate): Dialog = speech(
        speaker,
        listOf(
            say("Ho ho ho! Na, warst du denn auch artig?"),
            say("Oh je, mir ist etwas Schlimmes passiert: Auf dem Weg hierher habe ich all meine Weihnachtsgeschenke verloren! Jetzt liegen sie überall am Central Spawn verstreut."),
            say("Jeden Tag kannst du eines davon wiederfinden. Und keine Sorge: Hast du einen Tag verpasst, läuft dir das Geschenk nicht weg."),
            say("Hilfst du mir, meine Geschenke wiederzufinden?"),
        ),
    ) {
        label { success("Na klar, ich helfe dir!") }
        action { playerCallback { player -> player.showDialog(progress(speaker, status, today)) } }
    }

    /** Every later talk: the reward if it is ready, the thanks if it was given, otherwise the search. */
    fun progress(speaker: Component, status: NpcStatus.Active, today: LocalDate): Dialog = when (status.reward) {
        NpcReward.Ready -> speech(
            speaker,
            listOf(
                say("Ho ho ho! Du hast es geschafft! Du hast alle meine verlorenen Geschenke wiedergefunden und damit Weihnachten gerettet. Als Dank bekommst du von mir das 24. Geschenk! Frohe Weihnachten!"),
            ),
        ) {
            label { success("Geschenk annehmen") }
            action { playerCallback { player -> plugin.launch { NpcConversation.claimReward(player, speaker) } } }
        }

        NpcReward.Claimed -> farewell(speaker)
        else -> search(speaker, status, today)
    }

    /** After the reward was handed over. */
    fun thanks(speaker: Component): Dialog = speech(
        speaker,
        listOf(
            say("Ho ho ho! Ich hoffe, mein Geschenk gefällt dir."),
            say("Ohne dich wären die Kinder dieses Jahr leer ausgegangen. Danke, dass du Weihnachten gerettet hast!"),
            say("Ich und das gesamte Serverteam wünschen dir frohe Weihnachten und einen guten Rutsch ins neue Jahr!"),
        ),
    ) { label { success("Frohe Weihnachten!") } }

    /** When the player already received the reward. */
    private fun farewell(speaker: Component): Dialog = speech(
        speaker,
        listOf(
            say("Ho ho ho! Dank dir liegen alle Geschenke wieder unter dem Baum."),
            say("Ich und das gesamte Serverteam wünschen dir frohe Weihnachten und einen guten Rutsch ins neue Jahr!"),
        ),
    ) { label { success("Frohe Weihnachten!") } }

    /** When no event of this NPC is running. */
    fun resting(speaker: Component): Dialog = speech(
        speaker,
        listOf(say("Ho ho ho! Gerade gibt es bei mir nichts zu tun. Schau in der Weihnachtszeit wieder vorbei!")),
    ) { label { text("Bis bald!") } }

    /** While the gifts are still being searched: today's gift, older gifts to catch up and the progress. */
    private fun search(speaker: Component, status: NpcStatus.Active, today: LocalDate): Dialog {
        val reward = status.reward as? NpcReward.Waiting
        val allFound = status.total > 0 && status.found >= status.total
        val lines = buildList {
            when {
                // On or after the reward day: tell how many gifts are still missing for it.
                reward != null && reward.missing > 0 && !today.isBefore(reward.day) -> add(
                    say(
                        if (reward.missing == 1) {
                            "Ho ho ho! Mir fehlt noch ein Geschenk. Bring es mir zurück, dann bekommst du von mir das 24. Geschenk!"
                        } else {
                            "Ho ho ho! Mir fehlen noch ${reward.missing} Geschenke. Bring sie mir alle zurück, dann bekommst du von mir das 24. Geschenk!"
                        },
                    ),
                )

                // Everything found, only the reward day has not come yet.
                reward != null && reward.missing == 0 && allFound -> add(
                    say("Ho ho ho! Du hast alle meine verlorenen Geschenke gefunden! Komm am ${shortDay.format(reward.day)} wieder zu mir, dann habe ich eine Überraschung für dich."),
                )

                status.today == NpcToday.GIFT_HIDDEN -> add(
                    say("Ho ho ho! Du hast dein heutiges Geschenk noch nicht gefunden. Halte die Augen offen und hilf mir, meine Geschenke wiederzufinden!"),
                )

                status.today == NpcToday.GIFT_FOUND && status.openEarlier == 0 -> add(
                    say("Ho ho ho! Ruh dich erst einmal aus. Morgen kannst du weiter suchen!"),
                )

                status.today == NpcToday.GIFT_FOUND -> add(say("Ho ho ho! Dein heutiges Geschenk hast du schon gefunden, danke!"))
                else -> add(say("Ho ho ho! Heute habe ich zum Glück kein Geschenk verloren."))
            }
            if (status.openEarlier > 0 && !(reward != null && reward.missing > 0 && !today.isBefore(reward.day))) {
                add(
                    say(
                        if (status.openEarlier == 1) {
                            "Übrigens: Von den vergangenen Tagen liegt noch ein Geschenk herum. Das kannst du jederzeit nachholen!"
                        } else {
                            "Übrigens: Von den vergangenen Tagen liegen noch ${status.openEarlier} Geschenke herum. Die kannst du jederzeit nachholen!"
                        },
                    ),
                )
            }
            if (status.total > 0) {
                add(buildText {
                    variableKey("Gefundene Geschenke: ")
                    variableValue("${status.found}/${status.total}")
                })
            }
        }
        return speech(speaker, lines) { label { text("Bis bald!") } }
    }

    private fun say(text: String): Component = buildText { info(text) }

    /** A dialog titled [speaker] with one paragraph per line and a single [button]. */
    private fun speech(speaker: Component, lines: List<Component>, button: DialogActionButtonBuilder.() -> Unit): Dialog =
        dialog {
            base {
                title(speaker)
                body { lines.forEach { plainMessage(it, WIDTH) } }
            }
            type { notice(button) }
        }
}
