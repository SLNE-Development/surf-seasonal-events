package dev.slne.surf.seasonalevents.config.gift

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.util.Services
import java.util.*

@AutoService(GiftService::class)
class GiftServiceImpl : GiftService, Services.Fallback {

    private val _gifts = mutableObjectListOf<Gift>()
    override val gifts get() = _gifts.freeze()

    override val giftCount get() = gifts.size


    override fun registerGifts() {
        _gifts.clear()
        _gifts.addAll(giftConfig.gifts)
    }

    override fun registerGift(gift: Gift) {
        _gifts.add(gift)

        giftConfig.apply {
            gifts.add(gift)
        }
        GiftConfigHolder.save()
    }

    override fun unregisterGift(gift: Gift) {
        _gifts.remove(gift)

        giftConfig.apply {
            gifts.remove(gift)
        }
        GiftConfigHolder.save()
    }

    override fun getById(giftId: UUID): Gift? {
        return gifts.firstOrNull { it.giftId == giftId }
    }

    override fun getByName(giftName: String): Gift? {
        return gifts.firstOrNull { it.name.equals(giftName, ignoreCase = true) }
    }

    override fun generateUnusedId(): UUID {
        var id: UUID
        do {
            id = UUID.randomUUID()
        } while (_gifts.any { it.giftId == id })
        return id
    }
}