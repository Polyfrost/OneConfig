package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.migration.VigilanceMigrator;
import gg.essential.universal.UKeyboard;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TestConfig extends Config {

    @Switch(
            name = "Test Switch",
            size = 2
    )
    boolean testSwitch = false;

    @Checkbox(
            name = "Check box",
            size = 2
    )
    boolean testCheckBox = true;

    @Info(
            text = "Test Info",
            type = InfoType.ERROR,
            size = 2
    )
    boolean ignored;

    @Header(
            text = "Test Header",
            size = 2
    )
    boolean ignored1;

    @Dropdown(
            name = "Test Dropdown",
            options = {"option1", "option2", "option3"},
            size = 2
    )
    int testDropdown = 0;

    @Color(
            name = "Test Color",
            size =  2
    )
    OneColor testColor = new OneColor(0, 255, 255);

    @Text(
            name = "Test Text",
            size = 2
    )
    String testText = "Epic Text";

    @Button(
            name = "Test Button",
            text = "Crash game"
    )
    Runnable runnable = () -> FMLCommonHandler.instance().exitJava(69, false);

    @Slider(
            name = "Test Slider",
            min = 25,
            max = 50
    )
    float testSlider = 50;

    @KeyBind(
            name = "Test KeyBind",
            size = 2
    )
    OneKeyBind testKeyBind = new OneKeyBind(UKeyboard.KEY_LSHIFT, UKeyboard.KEY_S);

    @DualOption(
            name = "Test Dual Option",
            left = "YES",
            right = "NO",
            size = 2
    )
    boolean testDualOption = false;

    @Switch(
            name = "Test Switch",
            size = 2,
            category = "Category 2"
    )
    boolean testSwitch1 = false;

    @Switch(
            name = "Test Switch",
            size = 2,
            category = "Category 2",
            subcategory = "Test Subcategory"
    )
    boolean testSwitch2 = false;


    public TestConfig() {
        super(new Mod("Test Mod", ModType.UTIL_QOL, new VigilanceMigrator("./config/testConfig.toml")), "hacksConfig.json");
    }
}

