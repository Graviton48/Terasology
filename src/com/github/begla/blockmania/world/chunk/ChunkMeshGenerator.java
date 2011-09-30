/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.begla.blockmania.world.chunk;

import com.github.begla.blockmania.blocks.Block;
import com.github.begla.blockmania.blocks.BlockManager;
import com.github.begla.blockmania.main.Configuration;
import com.github.begla.blockmania.noise.PerlinNoise;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Generates tessellated chunk meshes from chunks.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class ChunkMeshGenerator {

    private final Chunk _chunk;
    private static final PerlinNoise _pGen = new PerlinNoise(0);
    private static int _statVertexArrayUpdateCount = 0;

    public ChunkMeshGenerator(Chunk chunk) {
        _chunk = chunk;
    }

    public ChunkMesh generateMesh() {
        ChunkMesh mesh = new ChunkMesh();

        for (int x = 0; x < Configuration.CHUNK_DIMENSIONS.x; x++) {
            for (int z = 0; z < Configuration.CHUNK_DIMENSIONS.z; z++) {
                double biomeTemp = _chunk.getParent().getTemperatureAt(_chunk.getBlockWorldPosX(x), _chunk.getBlockWorldPosZ(z));
                double biomeHumidity = _chunk.getParent().getHumidityAt(_chunk.getBlockWorldPosX(x), _chunk.getBlockWorldPosZ(z));

                for (int y = 0; y < Configuration.CHUNK_DIMENSIONS.y; y++) {
                    byte blockType = _chunk.getBlock(x, y, z);
                    Block block = BlockManager.getInstance().getBlock(blockType);

                    if (block.isBlockInvisible())
                        continue;

                    Block.BLOCK_FORM blockForm = block.getBlockForm();

                    if (blockForm == Block.BLOCK_FORM.LOWERED_BOCK || blockForm == Block.BLOCK_FORM.CACTUS || blockForm == Block.BLOCK_FORM.DEFAULT)
                        generateBlockVertices(mesh, x, y, z, biomeTemp, biomeHumidity);
                    else if (blockForm == Block.BLOCK_FORM.BILLBOARD)
                        generateBillboardVertices(mesh, x, y, z, biomeTemp, biomeHumidity);
                }
            }
        }

        generateOptimizedBuffers(mesh);
        _statVertexArrayUpdateCount++;

        return mesh;
    }

    private void generateOptimizedBuffers(ChunkMesh mesh) {
        for (int j = 0; j < mesh._vertexElements.length; j++) {
            mesh._vertexElements[j].vertices = BufferUtils.createFloatBuffer(mesh._vertexElements[j].quads.size() * 2 + mesh._vertexElements[j].tex.size() + mesh._vertexElements[j].color.size());
            mesh._vertexElements[j].indices = BufferUtils.createIntBuffer(mesh._vertexElements[j].quads.size());

            int cTex = 0;
            int cColor = 0;
            int cIndex = 0;
            for (int i = 0; i < mesh._vertexElements[j].quads.size(); i += 3, cTex += 2, cColor += 4) {

                if (i % 4 == 0) {
                    mesh._vertexElements[j].indices.put(cIndex);
                    mesh._vertexElements[j].indices.put(cIndex + 1);
                    mesh._vertexElements[j].indices.put(cIndex + 2);

                    mesh._vertexElements[j].indices.put(cIndex + 2);
                    mesh._vertexElements[j].indices.put(cIndex + 3);
                    mesh._vertexElements[j].indices.put(cIndex);
                    cIndex += 4;
                }

                Vector3f vertexPos = new Vector3f(mesh._vertexElements[j].quads.get(i), mesh._vertexElements[j].quads.get(i + 1), mesh._vertexElements[j].quads.get(i + 2));

                mesh._vertexElements[j].vertices.put(vertexPos.x);
                mesh._vertexElements[j].vertices.put(vertexPos.y);
                mesh._vertexElements[j].vertices.put(vertexPos.z);

                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].tex.get(cTex));
                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].tex.get(cTex + 1));

                double occlusionValue = getOcclusionValue(vertexPos);
                mesh._vertexElements[j].vertices.put((float) (getLightForVertexPos(vertexPos, Chunk.LIGHT_TYPE.SUN)));
                mesh._vertexElements[j].vertices.put((float) (getLightForVertexPos(vertexPos, Chunk.LIGHT_TYPE.BLOCK)));
                mesh._vertexElements[j].vertices.put((float) occlusionValue);

                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].color.get(cColor));
                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].color.get(cColor + 1));
                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].color.get(cColor + 2));
                mesh._vertexElements[j].vertices.put(mesh._vertexElements[j].color.get(cColor + 3));

            }

            mesh._vertexElements[j].vertices.flip();
            mesh._vertexElements[j].indices.flip();
        }
    }

    private double getLightForVertexPos(Vector3f vertexPos, Chunk.LIGHT_TYPE lightType) {
        double result = 0.0;

        double[] lights = new double[8];

        lights[0] = _chunk.getParent().getLight((int) (vertexPos.x + 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z + 0.5f), lightType) / 15f;
        lights[1] = _chunk.getParent().getLight((int) (vertexPos.x + 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z - 0.5f), lightType) / 15f;
        lights[2] = _chunk.getParent().getLight((int) (vertexPos.x - 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z - 0.5f), lightType) / 15f;
        lights[3] = _chunk.getParent().getLight((int) (vertexPos.x - 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z + 0.5f), lightType) / 15f;

        lights[4] = _chunk.getParent().getLight((int) (vertexPos.x + 0.5f), (int) (vertexPos.y - 0.5f), (int) (vertexPos.z + 0.5f), lightType) / 15f;
        lights[5] = _chunk.getParent().getLight((int) (vertexPos.x + 0.5f), (int) (vertexPos.y - 0.5f), (int) (vertexPos.z - 0.5f), lightType) / 15f;
        lights[6] = _chunk.getParent().getLight((int) (vertexPos.x - 0.5f), (int) (vertexPos.y - 0.5f), (int) (vertexPos.z - 0.5f), lightType) / 15f;
        lights[7] = _chunk.getParent().getLight((int) (vertexPos.x - 0.5f), (int) (vertexPos.y - 0.5f), (int) (vertexPos.z + 0.5f), lightType) / 15f;

        int counter = 0;
        for (int i = 0; i < 8; i++) {
            if (lights[i] > 0) {
                result += lights[i];
                counter++;
            }
        }

        if (counter == 0)
            return 0;

        return result / counter;
    }

    private double getOcclusionValue(Vector3f vertexPos) {

        double result = 1.0;
        byte[] blocks = new byte[8];

        blocks[0] = _chunk.getParent().getBlock((int) (vertexPos.x + 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z + 0.5f));
        blocks[1] = _chunk.getParent().getBlock((int) (vertexPos.x + 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z - 0.5f));
        blocks[2] = _chunk.getParent().getBlock((int) (vertexPos.x - 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z - 0.5f));
        blocks[3] = _chunk.getParent().getBlock((int) (vertexPos.x - 0.5f), (int) (vertexPos.y + 0.5f), (int) (vertexPos.z + 0.5f));

        for (int i = 0; i < 4; i++) {
            Block b = BlockManager.getInstance().getBlock(blocks[i]);
            if (b.isCastingShadows() && b.getBlockForm() != Block.BLOCK_FORM.BILLBOARD) {
                result -= Configuration.OCCLUSION_AMOUNT_DEFAULT;
            } else if (b.isCastingShadows() && b.getBlockForm() == Block.BLOCK_FORM.BILLBOARD) {
                result -= Configuration.OCCLUSION_AMOUNT_BILLBOARDS;
            }
        }

        return result;
    }

    /**
     * Generates the billboard vertices for a given local block position.
     *
     * @param mesh The active mesh
     * @param x    Local block position on the x-axis
     * @param y    Local block position on the y-axis
     * @param z    Local block position on the z-axis
     */
    private void generateBillboardVertices(ChunkMesh mesh, int x, int y, int z, double temp, double hum) {
        byte blockId = _chunk.getBlock(x, y, z);
        Block block = BlockManager.getInstance().getBlock(blockId);

        /*
         * First side of the billboard
         */
        Vector4f colorBillboardOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);
        Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.FRONT).x, block.getTextureOffsetFor(Block.SIDE.FRONT).y, 0);

        Vector3f p1 = new Vector3f(-0.5f, -0.5f, 0.5f);
        Vector3f p2 = new Vector3f(0.5f, -0.5f, -0.5f);
        Vector3f p3 = new Vector3f(0.5f, 0.5f, -0.5f);
        Vector3f p4 = new Vector3f(-0.5f, 0.5f, 0.5f);

        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p1));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p2));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p3));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p4));
        addBlockTextureData(mesh._vertexElements[2], texOffset, new Vector3f(0, 0, 1));

        /*
        * Second side of the billboard
        */
        colorBillboardOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);
        texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.BACK).x, block.getTextureOffsetFor(Block.SIDE.BACK).y, 0);

        p1 = new Vector3f(-0.5f, -0.5f, -0.5f);
        p2 = new Vector3f(0.5f, -0.5f, 0.5f);
        p3 = new Vector3f(0.5f, 0.5f, 0.5f);
        p4 = new Vector3f(-0.5f, 0.5f, -0.5f);

        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p1));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p2));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p3));
        addBlockVertexData(mesh._vertexElements[2], colorBillboardOffset, moveVectorToWorldSpace(x, y, z, p4));
        addBlockTextureData(mesh._vertexElements[2], texOffset, new Vector3f(0, 0, 1));
    }

    private void generateBlockVertices(ChunkMesh mesh, int x, int y, int z, double temp, double hum) {
        byte blockId = _chunk.getBlock(x, y, z);
        Block block = BlockManager.getInstance().getBlock(blockId);

        /*
         * Determine the render process.
         */
        ChunkMesh.RENDER_TYPE renderType = ChunkMesh.RENDER_TYPE.BILLBOARD_AND_TRANSLUCENT;

        if (!block.isBlockTypeTranslucent())
            renderType = ChunkMesh.RENDER_TYPE.OPAQUE;
        if (block.getTitle().equals("Water"))
            renderType = ChunkMesh.RENDER_TYPE.WATER;
        if (block.getTitle().equals("Lava"))
            renderType = ChunkMesh.RENDER_TYPE.LAVA;

        boolean drawFront, drawBack, drawLeft, drawRight, drawTop, drawBottom;

        byte blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y + 1, _chunk.getBlockWorldPosZ(z));
        drawTop = isSideVisibleForBlockTypes(blockToCheckId, blockId);
        blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y, _chunk.getBlockWorldPosZ(z - 1));
        drawFront = isSideVisibleForBlockTypes(blockToCheckId, blockId);
        blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y, _chunk.getBlockWorldPosZ(z + 1));
        drawBack = isSideVisibleForBlockTypes(blockToCheckId, blockId);
        blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x - 1), y, _chunk.getBlockWorldPosZ(z));
        drawLeft = isSideVisibleForBlockTypes(blockToCheckId, blockId);
        blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x + 1), y, _chunk.getBlockWorldPosZ(z));
        drawRight = isSideVisibleForBlockTypes(blockToCheckId, blockId);

        // Don't draw anything "below" the world
        if (y > 0) {
            blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y - 1, _chunk.getBlockWorldPosZ(z));
            drawBottom = isSideVisibleForBlockTypes(blockToCheckId, blockId);
        } else {
            drawBottom = false;
        }

        Block.BLOCK_FORM blockForm = block.getBlockForm();

        // If the block is lowered, some more faces have to be drawn
        if (blockForm == Block.BLOCK_FORM.LOWERED_BOCK) {
            blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y - 1, _chunk.getBlockWorldPosZ(z - 1));
            drawFront = isSideVisibleForBlockTypes(blockToCheckId, blockId) || drawFront;
            blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y - 1, _chunk.getBlockWorldPosZ(z + 1));
            drawBack = isSideVisibleForBlockTypes(blockToCheckId, blockId) || drawBack;
            blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x - 1), y - 1, _chunk.getBlockWorldPosZ(z));
            drawLeft = isSideVisibleForBlockTypes(blockToCheckId, blockId) || drawLeft;
            blockToCheckId = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x + 1), y - 1, _chunk.getBlockWorldPosZ(z));
            drawRight = isSideVisibleForBlockTypes(blockToCheckId, blockId) || drawRight;
        }

        if (drawTop) {
            Vector3f p1 = new Vector3f(-0.5f, 0.5f, 0.5f);
            Vector3f p2 = new Vector3f(0.5f, 0.5f, 0.5f);
            Vector3f p3 = new Vector3f(0.5f, 0.5f, -0.5f);
            Vector3f p4 = new Vector3f(-0.5f, 0.5f, -0.5f);

            Vector3f norm = new Vector3f(0, 1, 0);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.TOP).x, block.getTextureOffsetFor(Block.SIDE.TOP).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }

        if (drawFront) {

            Vector3f p1 = new Vector3f(-0.5f, 0.5f, -0.5f);
            Vector3f p2 = new Vector3f(0.5f, 0.5f, -0.5f);
            Vector3f p3 = new Vector3f(0.5f, -0.5f, -0.5f);
            Vector3f p4 = new Vector3f(-0.5f, -0.5f, -0.5f);

            Vector3f norm = new Vector3f(0, 0, -1);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.FRONT).x, block.getTextureOffsetFor(Block.SIDE.FRONT).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }

        if (drawBack) {
            Vector3f p1 = new Vector3f(-0.5f, -0.5f, 0.5f);
            Vector3f p2 = new Vector3f(0.5f, -0.5f, 0.5f);
            Vector3f p3 = new Vector3f(0.5f, 0.5f, 0.5f);
            Vector3f p4 = new Vector3f(-0.5f, 0.5f, 0.5f);

            Vector3f norm = new Vector3f(0, 0, 1);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.BACK).x, block.getTextureOffsetFor(Block.SIDE.BACK).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }

        if (drawLeft) {
            Vector3f p1 = new Vector3f(-0.5f, -0.5f, -0.5f);
            Vector3f p2 = new Vector3f(-0.5f, -0.5f, 0.5f);
            Vector3f p3 = new Vector3f(-0.5f, 0.5f, 0.5f);
            Vector3f p4 = new Vector3f(-0.5f, 0.5f, -0.5f);

            Vector3f norm = new Vector3f(-1, 0, 0);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.LEFT).x, block.getTextureOffsetFor(Block.SIDE.LEFT).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }

        if (drawRight) {
            Vector3f p1 = new Vector3f(0.5f, 0.5f, -0.5f);
            Vector3f p2 = new Vector3f(0.5f, 0.5f, 0.5f);
            Vector3f p3 = new Vector3f(0.5f, -0.5f, 0.5f);
            Vector3f p4 = new Vector3f(0.5f, -0.5f, -0.5f);

            Vector3f norm = new Vector3f(1, 0, 0);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.RIGHT).x,block.getTextureOffsetFor(Block.SIDE.RIGHT).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }

        if (drawBottom) {
            Vector3f p1 = new Vector3f(-0.5f, -0.5f, -0.5f);
            Vector3f p2 = new Vector3f(0.5f, -0.5f, -0.5f);
            Vector3f p3 = new Vector3f(0.5f, -0.5f, 0.5f);
            Vector3f p4 = new Vector3f(-0.5f, -0.5f, 0.5f);

            Vector3f norm = new Vector3f(0, -1, 0);

            Vector4f colorOffset = block.getColorOffsetFor(Block.SIDE.FRONT, temp, hum);

            Vector3f texOffset = new Vector3f(block.getTextureOffsetFor(Block.SIDE.BOTTOM).x, block.getTextureOffsetFor(Block.SIDE.BOTTOM).y, 0f);
            generateVerticesForBlockSide(mesh, x, y, z, p1, p2, p3, p4, norm, colorOffset, texOffset, renderType, blockForm);
        }
    }

    void generateVerticesForBlockSide(ChunkMesh mesh, int x, int y, int z, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, Vector3f norm, Vector4f colorOffset, Vector3f texOffset, ChunkMesh.RENDER_TYPE renderType, Block.BLOCK_FORM blockForm) {
        ChunkMesh.VertexElements vertexElements = mesh._vertexElements[0];

        switch (renderType) {
            case BILLBOARD_AND_TRANSLUCENT:
                vertexElements = mesh._vertexElements[1];
                break;
            case WATER:
                texOffset.set(0,0);
                vertexElements = mesh._vertexElements[3];
                break;
            case LAVA:
                texOffset.set(0,0);
                vertexElements = mesh._vertexElements[4];
                break;
        }

        switch (blockForm) {
            case CACTUS:
                generateCactusSide(p1, p2, p3, p4, norm);
                break;
            case LOWERED_BOCK:
                generateLoweredBlock(x, y, z, p1, p2, p3, p4, norm);
                break;
        }

        addBlockTextureData(vertexElements, texOffset, norm);

        addBlockVertexData(vertexElements, colorOffset, moveVectorToWorldSpace(x, y, z, p1));
        addBlockVertexData(vertexElements, colorOffset, moveVectorToWorldSpace(x, y, z, p2));
        addBlockVertexData(vertexElements, colorOffset, moveVectorToWorldSpace(x, y, z, p3));
        addBlockVertexData(vertexElements, colorOffset, moveVectorToWorldSpace(x, y, z, p4));
    }

    private Vector3f moveVectorToWorldSpace(int cPosX, int cPosY, int cPosZ, Vector3f offset) {
        double offsetX = _chunk.getPosition().x * Configuration.CHUNK_DIMENSIONS.x;
        double offsetY = _chunk.getPosition().y * Configuration.CHUNK_DIMENSIONS.y;
        double offsetZ = _chunk.getPosition().z * Configuration.CHUNK_DIMENSIONS.z;

        offset.x += offsetX + cPosX;
        offset.y += offsetY + cPosY;
        offset.z += offsetZ + cPosZ;

        return offset;
    }

    private void generateLoweredBlock(int x, int y, int z, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, Vector3f norm) {
        byte topBlock = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y + 1, _chunk.getBlockWorldPosZ(z));
        byte bottomBlock = _chunk.getParent().getBlock(_chunk.getBlockWorldPosX(x), y - 1, _chunk.getBlockWorldPosZ(z));

        boolean lower = topBlock == 0x0 || BlockManager.getInstance().getBlock(topBlock).getBlockForm() == Block.BLOCK_FORM.LOWERED_BOCK ;
        boolean lowerBottom = BlockManager.getInstance().getBlock(bottomBlock).getBlockForm() == Block.BLOCK_FORM.LOWERED_BOCK || bottomBlock == 0x0;

        if (!lower)
            return;

        if (norm.x == 1.0f) {
            p1.y -= 0.25;
            p2.y -= 0.25;

            if (lowerBottom) {
                p3.y -= 0.25;
                p4.y -= 0.25;
            }
        } else if (norm.x == -1.0f) {
            p3.y -= 0.25;
            p4.y -= 0.25;

            if (lowerBottom) {
                p1.y -= 0.25;
                p2.y -= 0.25;
            }
        } else if (norm.z == 1.0f) {
            p3.y -= 0.25;
            p4.y -= 0.25;

            if (lowerBottom) {
                p1.y -= 0.25;
                p2.y -= 0.25;
            }
        } else if (norm.z == -1.0f) {

            p1.y -= 0.25;
            p2.y -= 0.25;

            if (lowerBottom) {
                p3.y -= 0.25;
                p4.y -= 0.25;
            }
        } else if (norm.y == 1.0f) {
            p1.y -= 0.25;
            p2.y -= 0.25;
            p3.y -= 0.25;
            p4.y -= 0.25;
        } else if (norm.y == -1.0f) {
            if (lowerBottom) {
                p1.y -= 0.25;
                p2.y -= 0.25;
                p3.y -= 0.25;
                p4.y -= 0.25;
            }
        }
    }

    private void generateCactusSide(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, Vector3f norm) {
        if (norm.x == 1.0f || norm.x == -1.0f) {
            p1.x -= 0.0625 * norm.x;
            p2.x -= 0.0625 * norm.x;
            p3.x -= 0.0625 * norm.x;
            p4.x -= 0.0625 * norm.x;
        } else if (norm.z == 1.0f || norm.z == -1.0f) {
            p1.z -= 0.0625 * norm.z;
            p2.z -= 0.0625 * norm.z;
            p3.z -= 0.0625 * norm.z;
            p4.z -= 0.0625 * norm.z;
        }
    }

    private void addBlockTextureData(ChunkMesh.VertexElements vertexElements, Vector3f texOffset, Vector3f norm) {
        /*
        * Rotate the texture coordinates according to the
        * orientation of the plane.
        */
        if (norm.z == 1 || norm.x == -1) {
            vertexElements.tex.add(texOffset.x);
            vertexElements.tex.add(texOffset.y + 0.0624f);

            vertexElements.tex.add(texOffset.x + 0.0624f);
            vertexElements.tex.add(texOffset.y + 0.0624f);

            vertexElements.tex.add(texOffset.x + 0.0624f);
            vertexElements.tex.add(texOffset.y);

            vertexElements.tex.add(texOffset.x);
            vertexElements.tex.add(texOffset.y);
        } else {
            vertexElements.tex.add(texOffset.x);
            vertexElements.tex.add(texOffset.y);

            vertexElements.tex.add(texOffset.x + 0.0624f);
            vertexElements.tex.add(texOffset.y);

            vertexElements.tex.add(texOffset.x + 0.0624f);
            vertexElements.tex.add(texOffset.y + 0.0624f);

            vertexElements.tex.add(texOffset.x);
            vertexElements.tex.add(texOffset.y + 0.0624f);
        }
    }

    private void addBlockVertexData(ChunkMesh.VertexElements vertexElements, Vector4f colorOffset, Vector3f vertex) {
        vertexElements.color.add(colorOffset.x);
        vertexElements.color.add(colorOffset.y);
        vertexElements.color.add(colorOffset.z);
        vertexElements.color.add(colorOffset.w);
        vertexElements.quads.add(vertex.x);
        vertexElements.quads.add(vertex.y);
        vertexElements.quads.add(vertex.z);
    }

    /**
     * Returns true if the block side is adjacent to a translucent block or an air
     * block.
     *
     * @param blockToCheck The block to check
     * @param currentBlock The current block
     * @return True if the side is visible for the given block types
     */
    private boolean isSideVisibleForBlockTypes(byte blockToCheck, byte currentBlock) {
        Block bCheck = BlockManager.getInstance().getBlock(blockToCheck);
        Block cBlock = BlockManager.getInstance().getBlock(currentBlock);

        return bCheck.getId() == 0x0 || cBlock.doNotTessellate() || bCheck.getBlockForm() == Block.BLOCK_FORM.BILLBOARD || (!cBlock.isBlockTypeTranslucent() && bCheck.isBlockTypeTranslucent()) || (bCheck.getBlockForm() == Block.BLOCK_FORM.LOWERED_BOCK && cBlock.getBlockForm() != Block.BLOCK_FORM.LOWERED_BOCK);
    }

    public static int getVertexArrayUpdateCount() {
        return _statVertexArrayUpdateCount;
    }

}
