// RepoModel.java
package com.example.storytell.init.renderer;

import com.example.storytell.init.entity.REPO;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RepoModel extends HierarchicalModel<REPO> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("storytell", "repo"), "main");
    private final ModelPart root;
    private final ModelPart REPO;
    private final ModelPart leftarm;
    private final ModelPart leftleg;
    private final ModelPart rightleg;
    private final ModelPart rightarm;
    private final ModelPart heads;
    private final ModelPart body;

    public RepoModel(ModelPart root) {
        this.root = root;
        this.REPO = root.getChild("REPO");
        this.leftarm = this.REPO.getChild("leftarm");
        this.leftleg = this.REPO.getChild("leftleg");
        this.rightleg = this.REPO.getChild("rightleg");
        this.rightarm = this.REPO.getChild("rightarm");
        this.heads = this.REPO.getChild("heads");
        this.body = this.REPO.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // FIXED: Lowered model by 0.5 blocks (from 16.25F to 16.75F)
        PartDefinition REPO = partdefinition.addOrReplaceChild("REPO", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Left Arm
        PartDefinition leftarm = REPO.addOrReplaceChild("leftarm", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(13, 0).addBox(-1.0F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 0).addBox(-2.0F, 5.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, -3.0F, 0.0F));

        // Left Leg
        PartDefinition leftleg = REPO.addOrReplaceChild("leftleg", CubeListBuilder.create()
                        .texOffs(0, 35).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 31).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 5.75F, 0.0F));

        // Right Leg
        PartDefinition rightleg = REPO.addOrReplaceChild("rightleg", CubeListBuilder.create()
                        .texOffs(0, 35).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 31).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 5.75F, 0.0F));

        // Right Arm
        PartDefinition rightarm = REPO.addOrReplaceChild("rightarm", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, 0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(13, 0).addBox(0.0F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 0).addBox(1.0F, 5.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -3.0F, 0.0F));

        // Heads group
        PartDefinition heads = REPO.addOrReplaceChild("heads", CubeListBuilder.create(),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        // Head
        PartDefinition head = heads.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 0.5F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 25).addBox(-4.0F, -2.5F, -4.0F, 8.0F, 4.5F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Eyes
        PartDefinition eyes = heads.addOrReplaceChild("eyes", CubeListBuilder.create()
                        .texOffs(8, 31).addBox(0.5F, -1.5F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 31).addBox(-3.5F, -1.5F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -0.5F, -4.0F));

        // Body
        PartDefinition body = REPO.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(32, 25).addBox(-4.0F, -3.5F, -4.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Segment
        PartDefinition segment = body.addOrReplaceChild("segment", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-5.0F, 2.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 24).addBox(-5.0F, -3.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, -1.0F, 0.0F));

        // Lower Body
        PartDefinition dowbbody = body.addOrReplaceChild("dowbbody", CubeListBuilder.create()
                        .texOffs(0, 37).addBox(-5.5F, -1.25F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 10).addBox(-6.0F, -3.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 5.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(REPO entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Apply body rotation to the entire model
        this.REPO.yRot = entity.getBodyYRot() * ((float)Math.PI / 180F);

        // Apply head rotation separately for natural head movement
        this.heads.yRot = (netHeadYaw - entity.getBodyYRot()) * ((float)Math.PI / 180F);
        this.heads.xRot = headPitch * ((float)Math.PI / 180F);

        // Walking animation only when moving
        boolean isMoving = limbSwingAmount > 0.01F;

        if (isMoving) {
            float animationTime = ageInTicks * 0.5F;

            // Right arm animation
            this.rightarm.xRot = Mth.cos(animationTime) * 0.3F;

            // Left arm animation (opposite phase)
            this.leftarm.xRot = Mth.cos(animationTime + (float)Math.PI) * 0.3F;

            // Right leg animation
            this.rightleg.xRot = Mth.cos(animationTime + (float)Math.PI) * 0.25F;

            // Left leg animation
            this.leftleg.xRot = Mth.cos(animationTime) * 0.25F;
        } else {
            // Reset animation to default pose
            this.rightarm.xRot = 0.0F;
            this.leftarm.xRot = 0.0F;
            this.rightleg.xRot = 0.0F;
            this.leftleg.xRot = 0.0F;
        }
    }
}