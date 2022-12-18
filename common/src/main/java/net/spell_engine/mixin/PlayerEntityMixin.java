package net.spell_engine.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.animation.AnimatablePlayer;
import net.spell_engine.internals.*;
import net.spell_engine.runes.RuneCrafter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.spell_engine.api.spell.Spell.Release.Target.Type.BEAM;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements SpellCasterEntity, RuneCrafter {
    private PlayerEntity player() {
        return (PlayerEntity) ((Object) this);
    }

    private Identifier currentSpell;
    private final SpellCooldownManager spellCooldownManager = new SpellCooldownManager(player());

    public void setCurrentSpell(Identifier spellId) {
        currentSpell = spellId;
    }

    @Override
    public Identifier getCurrentSpellId() {
        if (player().isUsingItem()) {
            return currentSpell;
        }
        return null;
    }

    @Override
    public Spell getCurrentSpell() {
        if (player().isUsingItem()) {
            return SpellRegistry.getSpell(currentSpell);
        }
        return null;
    }

    @Override
    public float getCurrentCastProgress() {
        var spell = getCurrentSpell();
        if (spell != null) {
            return SpellHelper.getCastProgress(player(), player().getItemUseTimeLeft(), spell);
        }
        return 0;
    }

    @Override
    public SpellCooldownManager getCooldownManager() {
        return spellCooldownManager;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick_TAIL_SpellEngine(CallbackInfo ci) {
        lastRuneCrafted += 1;
        var player = player();
        if (player.world.isClient) {
            ((AnimatablePlayer)player()).updateSpellCastAnimationsOnTick();
            if (!player.isUsingItem() && currentSpell != null) {
            }
        } else {
            // Server side
            if (!player.isUsingItem() || SpellContainerHelper.containerFromItemStack(player.getActiveItem()) == null) {
                SpellCastSyncHelper.clearCasting(player);
            }
        }
        spellCooldownManager.update();
    }

    public boolean isBeaming() {
        return getBeam() != null;
    }

    @Nullable
    public Spell.Release.Target.Beam getBeam() {
        var spell = getCurrentSpell();
        if (spell != null && spell.on_release != null && spell.on_release.target.type == BEAM) {
            return spell.on_release.target.beam;
        }
        return null;
    }

    // MARK: RuneCrafter

    private int lastRuneCrafted = 0;

    @Override
    public void setLastCrafted(int time) {
        lastRuneCrafted = time;
    }

    @Override
    public int getLastCrafted() {
        return lastRuneCrafted;
    }
}
