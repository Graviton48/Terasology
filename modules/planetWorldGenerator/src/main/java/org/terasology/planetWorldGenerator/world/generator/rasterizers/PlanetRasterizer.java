
package org.terasology.planetWorldGenerator.world.generator.rasterizers;

import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizer;
import org.terasology.world.generation.facets.SurfaceHeightFacet;


public class PlanetRasterizer implements WorldRasterizer {
    private Block dirt;
    private Block grass;
    private Block snow;

    @Override
    public void initialize() {
        dirt = CoreRegistry.get(BlockManager.class).getBlock("Core:Dirt");
        grass = CoreRegistry.get(BlockManager.class).getBlock("Core:Grass");
        snow = CoreRegistry.get(BlockManager.class).getBlock("Core:Snow");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        SurfaceHeightFacet surfaceHeightFacet = chunkRegion.getFacet(SurfaceHeightFacet.class);
        for (Vector3i position : chunkRegion.getRegion()) {
            float surfaceHeight = surfaceHeightFacet.getWorld(position.x, position.z);
            if (position.y < surfaceHeight){
                chunk.setBlock(ChunkMath.calcBlockPos(position), snow);
            }
            else if (position.y < surfaceHeight - 50) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), grass);
            }
            else if (position.y < surfaceHeight - 100) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), dirt);
            }
        }
    }

}