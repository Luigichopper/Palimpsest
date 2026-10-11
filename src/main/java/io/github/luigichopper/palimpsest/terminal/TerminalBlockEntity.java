package io.github.luigichopper.palimpsest.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TerminalBlockEntity extends BlockEntity {
    private String customNodeId = "recovery-node-01";

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TERMINAL_BLOCK_ENTITY_TYPE, pos, state);
    }

    public String getCustomNodeId() {
        return customNodeId;
    }

    public void setCustomNodeId(String customNodeId) {
        this.customNodeId = customNodeId;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.customNodeId = input.getStringOr("node_id", "recovery-node-01");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("node_id", this.customNodeId);
    }
}
