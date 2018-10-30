import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldBuilder;
import org.terasology.world.generation.WorldRasterizer;

public class PlanetWorldRasterizer implements WorldRasterizer {
    private Block dirt;

    @Override
    public void initialize() {
        dirt = CoreRegistry.get(BlockManager.class).getBlock("Core:Dirt");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        for (Vector3i position : chunkRegion.getRegion()) {
            if (position.y < 0) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), dirt);
            }
        }
    }
}

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(worldGeneratorPluginLibrary)
                .addRasterizer(new PlanetWorldRasterizer());
    }