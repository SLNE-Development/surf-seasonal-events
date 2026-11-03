package dev.slne.surf.seasonalevents.commands.arguments

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.seasonalevents.config.gift.GiftService
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText

class GiftArgument(nodeName: String) :
    CustomArgument<Gift, String>(StringArgument(nodeName), { info ->
        GiftService.getByName(info.currentInput) ?: throw CustomArgumentException.fromAdventureComponent(
            buildText {
                appendErrorPrefix()
                error("Das Geschenk")
                appendSpace()
                variableValue(info.input)
                appendSpace()
                error("wurde nicht gefunden.")
            })
    }) {
    init {
        this.replaceSuggestions(
            ArgumentSuggestions.stringCollection {
                GiftService.gifts.map { it.name }
            }
        )
    }
}

inline fun CommandTree.giftArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(
    GiftArgument(nodeName).setOptional(optional).apply(block)
)

inline fun Argument<*>.giftArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> = then(
    GiftArgument(nodeName).setOptional(optional).apply(block)
)

inline fun CommandAPICommand.giftArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(GiftArgument(nodeName).setOptional(optional).apply(block))