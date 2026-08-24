package com.lnatit.womm.mixin;

import com.lnatit.womm.WOMMClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PauseScreen.class)
public class MixinPauseScreen
{
    @Shadow
    @Final
    private static int BUTTON_WIDTH_HALF;

    @Redirect(
            method = "createPauseMenu",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1
            )
    )
    private <T extends LayoutElement> T redirectDisconnectButton(
            GridLayout.RowHelper instance,
            T widget,
            int columnWidth
    ) {
        if (WOMMClient.isCallbackEmpty() || !(widget instanceof Button)) {
            return instance.addChild(widget, columnWidth);
        }

        ((Button) widget).setWidth(BUTTON_WIDTH_HALF);
        instance.addChild(
                Button.builder(
                              WOMMClient.returnText,
                              WOMMClient::returnToWorld
                      )
                      .width(BUTTON_WIDTH_HALF)
                      .build()
        );
        return instance.addChild(widget);
    }
}
