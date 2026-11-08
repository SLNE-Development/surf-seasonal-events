package dev.slne.surf.seasonalevents.paper.util.permission

import dev.slne.surf.api.paper.permission.PermissionRegistry

/** Permissions of `/seasonalevents`; every subcommand has its own node below the base command. */
object SeasonalEventsPermissions : PermissionRegistry() {
    private const val PREFIX = "surf.seasonal-events"
    private const val COMMAND = "$PREFIX.command"

    /** Base permission; required for every subcommand. */
    val COMMAND_SEASONAL_EVENTS = create("$COMMAND.seasonal-events")

    val COMMAND_LIST = create("$COMMAND_SEASONAL_EVENTS.list")
    val COMMAND_INFO = create("$COMMAND_SEASONAL_EVENTS.info")
    val COMMAND_CREATE = create("$COMMAND_SEASONAL_EVENTS.create")

    /** Display name, end date and marking blocks. */
    val COMMAND_EDIT = create("$COMMAND_SEASONAL_EVENTS.edit")
    val COMMAND_LOOT = create("$COMMAND_SEASONAL_EVENTS.loot")
    val COMMAND_START = create("$COMMAND_SEASONAL_EVENTS.start")
    val COMMAND_STOP = create("$COMMAND_SEASONAL_EVENTS.stop")
    val COMMAND_RESTORE = create("$COMMAND_SEASONAL_EVENTS.restore")
    val COMMAND_DELETE = create("$COMMAND_SEASONAL_EVENTS.delete")
}
