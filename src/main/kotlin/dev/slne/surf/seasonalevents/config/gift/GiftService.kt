package dev.slne.surf.seasonalevents.config.gift

import dev.slne.surf.surfapi.core.api.util.requiredService
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jetbrains.annotations.Unmodifiable
import java.util.*

private val giftService = requiredService<GiftService>()

interface GiftService {
    val giftCount: Int
    val gifts: @Unmodifiable ObjectList<Gift>

    fun registerGifts()
    fun registerGift(gift: Gift)
    fun unregisterGift(gift: Gift)

    fun getById(giftId: UUID): Gift?
    fun getByName(giftName: String): Gift?
    fun generateUnusedId(): UUID


    companion object : GiftService by giftService
}