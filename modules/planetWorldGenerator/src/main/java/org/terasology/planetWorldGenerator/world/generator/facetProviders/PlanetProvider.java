import org.terasology.core.world.generator.facetProviders.SeaLevelProvider;
import org.terasology.math.ChunkMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.*;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;

@Produces(SurfaceHeightFacet.class)
public class SurfaceProvider implements FacetProvider {
    @Override
    public void setSeed(long seed) {
    }

    @Override
    public void process(GeneratingRegion region) {
    }
}

@Override
    public void process(GeneratingRegion region) {
        // Create our surface height facet (we will get into borders later)
        Border3D border = region.getBorderForFacet(SurfaceHeightFacet.class);
        SurfaceHeightFacet facet = new SurfaceHeightFacet(region.getRegion(), border);

        // Loop through every position in our 2d array
        Rect2i processRegion = facet.getWorldRegion();
        for (BaseVector2i position: processRegion.contents()) {
            facet.setWorld(position, 10f);
        }

        // Pass our newly created and populated facet to the region
        region.setRegionFacet(SurfaceHeightFacet.class, facet);
    }

    @In
    private WorldGeneratorPluginLibrary worldGeneratorPluginLibrary;

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(worldGeneratorPluginLibrary)
                .addProvider(new SurfaceProvider())
                .addProvider(new SeaLevelProvider(0))
                .addRasterizer(new PlanetWorldRasterizer());
    }
    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        SurfaceHeightFacet surfaceHeightFacet = chunkRegion.getFacet(SurfaceHeightFacet.class);
        for (Vector3i position : chunkRegion.getRegion()) {
            float surfaceHeight = surfaceHeightFacet.getWorld(position.x, position.z);
            if (position.y < surfaceHeight) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), dirt);
            }
        }
    }
