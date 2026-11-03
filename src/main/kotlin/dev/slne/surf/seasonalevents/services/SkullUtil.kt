package dev.slne.surf.seasonalevents.services


import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
object SkullUtil {
    private val presenttextures = listOf(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFlYzljYWJkMDNkNmYyZWIxM2U0OTI3MWQwOWIwYTZiNjA3YzFiYWNhZmViMmU4MmU3NTkyMjk2YmE2ZTk5OCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzNlYmQwOWE4MGM3Y2QwOGI2MmMwNThiNGNlNTgxMTU2MmM3MzU0YWFkMjc1M2Q1YmQ4ZWUzNGExMTRmYmQ2MCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRjMWI3NGI1NmE3ZmU3MjJhNjJjMzQ1N2FmYjViOWFjOWUyMTQwNTk0YzVhZGQ3OTY2ODVhYTY1ZTMzYzVmOSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI5MjVhNmU5NzU5YTJjMWNjMjhiOTBkYjNjN2RkOTlhZmI0ZWNhZWIzNjk2OTVlZmFlZTM1MzEwNGY1NzE2YyJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjkzM2IxZWMxZDhlOTgyMmYxNWUxOGRiODM5OTM3YjViZGY1ZWI3NDBjZTc5OGZjOTNjNTE3MDFiN2ZmNGNiNSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmYxMjYxOWU2MjNhZjI1ZGFhYjdkMTEzMGNjODdkNjM2YTdlYzJiZTc3NjAyYmI5YTI2MDJkYjlmNGI2NDczZiJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ0YmQxMjAzNjYyNTkwNjhlNmM0MDE5ODQzZmFlMzU3NDEyODc1NTU4MDM1ZjFkMzYyMjY4YmUwNGZkYzVjOCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTViOTA2MDYyNjIzYjFmNjM3YWY3ZTNmZTM0Y2Q5NDc3NzlmNWYyNGQyNzI3ZDY2M2JjMjY2MzI3YzVjYjdiOSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI5MDQxMDViMzg0YTJmMzA3NjQ5ODg0ZjVhNWY1NmE1MGQ3OGRkZTdjNjk0YzliMTQ1NWRjNWM3YTJiZTYwZiJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjUwZDhlZDQ2ZjMxNTlkNmEyNzUwN2U3MjlkZjUwYTQ0ZGYyZTBiZWNlNjc1MTU0ZDgzZGI0ZmM0Y2FhY2IwYiJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTAyZDRjMGY4ZmFkOGRjMzhlNGVjODdhYWY0YmQzMmYyZmIyOTYwZTE3NmU5Nzk4YzdlYTI4NTkzZGU4MmM2MSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGMyYTEyMDZlOWE5M2Y4MDM5NTdmZDZjZGQwNjZhYmFhZjM4NTVkZWU2Yzc1YjcyYjAxNjQ5YmFjZjllNTdmOSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTBkZmIyY2ViNzQ0NGYzYmE5NDY5ZWYzMTk5M2MyYWEyMDM1NDIyYTE0ZjQwNGI1NmJkMGI2ODZlMDU5NTBiOCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk1MTZmNWMyYTY0NGFhMmE0NWVlYjZlOGFkYTc5MzNlMGQ5ZGU1NDA2MDExN2UyZDBlZDQzZThjNjcwNDdiMyJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTBmOTIyYWY2OWJkYTMwYzRmYWQzMWM2ZTE3ZjkxY2M0MWI2ZmQ3OTYzMzMzODY3NDRlNmIxYmEyYmVjZjZlNCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTg0ODNjM2QwMGNkYmZhOGM4MTg4YWNlYjY3Y2QwYTNhYzgzNTMwMjNmMTQxYjg1ZTFhZmVhMjdiZTAxYTMwNSJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjQ3MzJiN2M5NTYxYmNjNmNiZWVmMDM2YzE0NWQzYmU4YTk1ZGNmODBhOTk4N2UwYmQzMjdhZGVlMzY2ZmRlNyJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmE0MDc2NmI0ZjQyZDk1OGRjNTJlZjY5NWQwOGMzMzRiNmFmZGRmNTZhZTJmOTE1NTNhMDlhMWQ5OGEwYTJhNiJ9fX0="
    )

    fun getRandomPresentHead(): ItemStack {
        return createSkull(presenttextures.random())
    }

    private fun createSkull(texture: String) = buildItem(Material.PLAYER_HEAD) {
        setData(
            DataComponentTypes.PROFILE,
            ResolvableProfile.resolvableProfile().addProperty(
                ProfileProperty("textures", texture)
            ).build()
        )
        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.PROFILE)
                .build()
        )
    }
}

