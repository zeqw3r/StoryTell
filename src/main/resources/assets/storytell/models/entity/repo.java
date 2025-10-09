// Made with Blockbench 4.12.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class repo<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "repo"), "main");
	private final ModelPart bb_main;

	public repo(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(28, 30).addBox(-4.0F, -3.0F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(1.0F, -3.0F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.0F, -12.0F, -4.0F, 10.0F, 9.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(36, 6).addBox(-7.0F, -10.0F, -2.0F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(36, 6).addBox(5.0F, -10.0F, -2.0F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(-4.0F, -13.0F, -3.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 17).addBox(-5.0F, -18.0F, -4.0F, 10.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(-4.0F, -19.0F, -3.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}