package ar.edu.uade.skillswap;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.infrastructure.WiredUiControllers;
import ar.edu.uade.skillswap.infrastructure.SkillSwapApplicationWiring;
import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class SkillSwapApplication {
    private SkillSwapApplication() {}

    public static void main(String[] args) throws Exception {
        Theme.install();
        boolean persistent = java.util.Arrays.asList(args).contains("--persistence=jpa");
        String[] viewArgs = java.util.Arrays.stream(args)
                .filter(arg -> !arg.equals("--persistence=jpa")).toArray(String[]::new);
        SkillSwapApplicationWiring wiring = persistent
                ? SkillSwapApplicationWiring.persistent() : new SkillSwapApplicationWiring();
        Runtime.getRuntime().addShutdownHook(new Thread(wiring::close, "skillswap-persistence-close"));
        UiControllers controllers = new WiredUiControllers(wiring);
        if (viewArgs.length == 2 && "--screenshot".equals(viewArgs[0])) {
            SkillSwapView view = new SkillSwapView(controllers);
            view.setSize(1365, 768); view.doLayout(); layoutTree(view);
            BufferedImage image = new BufferedImage(1365,768,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);view.printAll(g);g.dispose();
            try { ImageIO.write(image,"png",new File(viewArgs[1])); }
            finally { wiring.close(); }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame=new JFrame(persistent ? "SkillSwap · MySQL" : "SkillSwap · Mock UI Kit");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1120,700));frame.setSize(1365,820);frame.setLocationRelativeTo(null);
            frame.setContentPane(new SkillSwapView(controllers));frame.setVisible(true);
        });
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for(Component x:c.getComponents()) if(x instanceof Container child) layoutTree(child);
    }
}
