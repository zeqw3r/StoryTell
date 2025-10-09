// REPORenderer.java
package com.example.storytell.init.renderer;

import com.example.storytell.init.entity.REPO;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class REPORenderer extends MobRenderer<REPO, RepoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("storytell", "textures/entity/repo.png");

    public REPORenderer(EntityRendererProvider.Context context) {
        super(context, new RepoModel(context.bakeLayer(RepoModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(REPO entity) {
        return TEXTURE;
    }
}