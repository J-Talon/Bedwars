package me.camm.productions.bedwars.Items.ItemDatabases;

public enum InventoryLocation
{
    HOT_BAR_START(0),
    HOT_BAR_END(9),

    LARGE_ROW_ONE_END(8),
    LARGE_ROW_ONE_START(0),

    LARGE_ROW_TWO_END(17),
    LARGE_ROW_TWO_START(9),

    LARGE_ROW_THREE_END(26),
    LARGE_ROW_THREE_START(18),

    LARGE_ROW_FOUR_END(35),
    LARGE_ROW_FOUR_START(27),

    LARGE_ROW_FIVE_END(44),
    LARGE_ROW_FIVE_START(36),

    LARGE_ROW_SIX_END(53),
    LARGE_ROW_SIX_START(45),

    QUICK_INV_NAV_START(0),
    QUICK_INV_NAV_END(9),


    //The slots of the separators in the inv.
    QUICK_INV_SEPARATE_START(9),
    QUICK_INV_SEPARATE_END(17),



    //Slots of the items in the inv. (Ones you can buy.)
    QUICK_INV_BORDER_START(19),
    QUICK_INV_BORDER_ROW2_START(28),
    QUICK_INV_BORDER_END(43),

    //28
    SHOP_SIZE(54);

    private final int value;

    InventoryLocation(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

}