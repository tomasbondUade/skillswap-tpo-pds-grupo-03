package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ar.edu.uade.skillswap.ui.Theme.*;

public final class SkillSwapView extends JPanel {
    private String USER_ID="user-mateo";
    private final UiControllers controllers;
    private final UiActionHandler actions = new UiActionHandler(error ->
            JOptionPane.showMessageDialog(this, error.mensaje(),
                    "No se pudo completar la operación", JOptionPane.ERROR_MESSAGE));
    private final JPanel content=new JPanel(new BorderLayout());
    private final JLabel pageTitle=label("Inicio",H1,TEXT);
    private final JLabel badge=label("",new Font("SansSerif",Font.BOLD,10),Color.WHITE);

    public SkillSwapView(UiControllers controllers) {
        this.controllers=controllers; setLayout(new BorderLayout()); setBackground(BG);
        actions.run(() -> {
            add(sidebar(),BorderLayout.WEST); add(shell(),BorderLayout.CENTER); showHome();
        });
    }

    private void switchDemoUser() {
        if (controllers.sesionDemo() == null) return;
        String[] options = {"user-mateo", "mentor-ana", "mentor-lu"};
        String selected = (String) JOptionPane.showInputDialog(this, "Usuario de demostración",
                "Cambiar identidad", JOptionPane.QUESTION_MESSAGE, null, options, USER_ID);
        if (selected == null) return;
        controllers.sesionDemo().cambiarUsuario(selected);
        USER_ID = selected;
        showHome();
    }

    private JComponent sidebar() {
        JPanel side=new JPanel(); side.setBackground(NAVY); side.setPreferredSize(new Dimension(228,720)); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS)); side.setBorder(new EmptyBorder(24,16,18,16));
        JLabel logo=label("SkillSwap",new Font("SansSerif",Font.BOLD,25),Color.WHITE); logo.setAlignmentX(LEFT_ALIGNMENT); side.add(logo);
        JLabel sub=label("Aprendé. Compartí. Crecé.",SMALL,new Color(177,200,216)); sub.setAlignmentX(LEFT_ALIGNMENT); side.add(sub); side.add(Box.createVerticalStrut(28));
        side.add(nav("⌂  Inicio",this::showHome)); side.add(nav("⌕  Explorar",this::showExplore)); side.add(nav("▣  Mis publicaciones",this::showListings)); side.add(nav("✓  Contrataciones",this::showContracts)); side.add(nav("◈  SkillCoins",this::showWallet)); side.add(nav("●  Notificaciones",this::showNotifications)); side.add(nav("♙  Mi perfil",this::showProfile));side.add(nav("⇄  Cambiar usuario demo",this::switchDemoUser)); side.add(Box.createVerticalGlue());
        JPanel user=new JPanel(new BorderLayout(10,0)); user.setOpaque(false); user.setBorder(new EmptyBorder(8,6,4,4)); user.setMaximumSize(new Dimension(Integer.MAX_VALUE,56)); user.setAlignmentX(LEFT_ALIGNMENT);
        JLabel avatar=avatar("MS",36); user.add(avatar,BorderLayout.WEST); JPanel txt=new JPanel();txt.setOpaque(false);txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));txt.add(label("Mateo Silva",H3,Color.WHITE));txt.add(label("Software Engineer",SMALL,new Color(177,200,216)));user.add(txt);side.add(user); return side;
    }

    private JButton nav(String text,Runnable action){JButton b=new JButton(text);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.setHorizontalAlignment(SwingConstants.LEFT);b.setForeground(new Color(224,234,241));b.setBackground(NAVY);b.setBorder(new EmptyBorder(10,12,10,12));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(uiAction(e->action.run()));return b;}

    private JComponent shell(){JPanel shell=new JPanel(new BorderLayout());shell.setBackground(BG);JPanel top=new JPanel(new BorderLayout());top.setBackground(Color.WHITE);top.setBorder(new EmptyBorder(16,26,16,26));top.add(pageTitle,BorderLayout.WEST);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);badge.setOpaque(true);badge.setBackground(DANGER);badge.setBorder(new EmptyBorder(5,8,5,8));actions.add(badge);JButton coins=button("◈  "+controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",false);coins.addActionListener(uiAction(e->showWallet()));actions.add(coins);top.add(actions,BorderLayout.EAST);shell.add(top,BorderLayout.NORTH);content.setBackground(BG);shell.add(content);return shell;}

    private void page(String title,JComponent view){pageTitle.setText(title);long unread=controllers.notificaciones().listarNotificaciones(USER_ID).stream().filter(n->!n.leida()).count();badge.setText(unread+" NUEVAS");content.removeAll();JScrollPane sp=new JScrollPane(view);sp.setBorder(null);sp.getVerticalScrollBar().setUnitIncrement(18);sp.getViewport().setBackground(BG);content.add(sp);content.revalidate();content.repaint();}

    private JPanel base(){JPanel p=new JPanel();p.setBackground(BG);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(24,26,28,26));return p;}
    private void addSection(JPanel p,String title,String subtitle){JLabel h=label(title,H2,TEXT);h.setAlignmentX(LEFT_ALIGNMENT);p.add(h);if(subtitle!=null){JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(LEFT_ALIGNMENT);p.add(Box.createVerticalStrut(3));p.add(s);}p.add(Box.createVerticalStrut(14));}

    private void showHome(){JPanel p=base();JPanel hero=new Theme.RoundedPanel(22,NAVY_2);hero.setLayout(new BorderLayout(22,0));hero.setBorder(new EmptyBorder(24,26,24,26));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));JPanel words=new JPanel();words.setOpaque(false);words.setLayout(new BoxLayout(words,BoxLayout.Y_AXIS));words.add(label("Hola, Mateo 👋",new Font("SansSerif",Font.BOLD,25),Color.WHITE));words.add(Box.createVerticalStrut(7));words.add(label("¿Qué habilidad querés desbloquear hoy?",BODY,new Color(213,228,238)));words.add(Box.createVerticalStrut(16));JTextField search=new JTextField();search.putClientProperty("JTextField.placeholderText","Buscar mentorías, servicios o habilidades...");search.setBorder(new EmptyBorder(11,13,11,13));search.addActionListener(uiAction(e->showExplore(search.getText())));words.add(search);hero.add(words);JPanel stat=new JPanel();stat.setOpaque(false);stat.setLayout(new BoxLayout(stat,BoxLayout.Y_AXIS));stat.add(label("TU PROGRESO",SMALL,new Color(169,202,219)));stat.add(label("52",new Font("SansSerif",Font.BOLD,34),Color.WHITE));stat.add(label("proyectos completados",SMALL,new Color(213,228,238)));hero.add(stat,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(26));addSection(p,"Recomendado para vos","Selecciones basadas en tus habilidades e intereses");p.add(cardGrid(controllers.busqueda().recomendar(USER_ID)));p.add(Box.createVerticalStrut(26));addSection(p,"Explorá por categoría",null);JPanel cats=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));cats.setOpaque(false);for(String c:controllers.busqueda().categorias())if(!c.equals("Todas")){JButton b=button(c,false);b.addActionListener(uiAction(e->showExploreCategory(c)));cats.add(b);}cats.setAlignmentX(LEFT_ALIGNMENT);p.add(cats);page("Inicio",p);}

    private void showExplore(){showExplore("");}
    private void showExplore(String initial){showExploreWith(initial,"Todas");}
    private void showExploreCategory(String category){showExploreWith("",category);}
    private void showExploreWith(String initial,String category){JPanel p=base();JPanel filters=card(new FlowLayout(FlowLayout.LEFT,10,0));filters.setAlignmentX(LEFT_ALIGNMENT);JTextField q=new JTextField(initial,24);q.setBorder(new EmptyBorder(10,12,10,12));JComboBox<String> cat=new JComboBox<>(controllers.busqueda().categorias().toArray(String[]::new));cat.setSelectedItem(category);JComboBox<String> order=new JComboBox<>(new String[]{"Relevancia","Mejor reputación","Menor precio"});JButton go=button("Buscar",true);filters.add(q);filters.add(cat);filters.add(order);filters.add(go);p.add(filters);p.add(Box.createVerticalStrut(22));JPanel results=new JPanel();results.setOpaque(false);results.setLayout(new BoxLayout(results,BoxLayout.Y_AXIS));Runnable refresh=()->{results.removeAll();List<Publicacion> list=controllers.busqueda().buscar(new FiltrosBusqueda(q.getText(),(String)cat.getSelectedItem(),null,0,(String)order.getSelectedItem()));addSection(results,list.size()+" resultados","Mentorías y servicios disponibles");results.add(cardGrid(list));results.revalidate();results.repaint();};go.addActionListener(uiAction(e->refresh.run()));q.addActionListener(uiAction(e->refresh.run()));p.add(results);refresh.run();page("Explorar",p);}

    private JPanel cardGrid(List<Publicacion> list){JPanel grid=new JPanel(new GridLayout(0,3,14,14));grid.setOpaque(false);grid.setAlignmentX(LEFT_ALIGNMENT);for(Publicacion x:list)grid.add(listingCard(x));return grid;}
    private JComponent listingCard(Publicacion x){JPanel c=card(new BorderLayout(0,12));JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(pill(x.categoria(),TEAL),BorderLayout.WEST);top.add(label("★ "+x.reputacion(),H3,GOLD),BorderLayout.EAST);c.add(top,BorderLayout.NORTH);JPanel mid=new JPanel();mid.setOpaque(false);mid.setLayout(new BoxLayout(mid,BoxLayout.Y_AXIS));JLabel t=label("<html><div style='width:210px'>"+x.titulo()+"</div></html>",H3,TEXT);mid.add(t);mid.add(Box.createVerticalStrut(7));mid.add(label("Por "+x.proveedor(),SMALL,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label("<html><div style='width:215px;color:#677480'>"+x.descripcion()+"</div></html>",BODY,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label(x.modalidad()+"  ·  "+x.duracionMinutos()+" min",SMALL,MUTED));c.add(mid);JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(label(x.precio()+" ◈",new Font("SansSerif",Font.BOLD,20),NAVY),BorderLayout.WEST);JButton detail=button("Ver detalle",true);detail.addActionListener(uiAction(e->showDetail(x)));bottom.add(detail,BorderLayout.EAST);c.add(bottom,BorderLayout.SOUTH);return c;}

    private void showDetail(Publicacion publication) {
        JPanel page = base();
        JButton back = button("← Volver a explorar", false);
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.addActionListener(uiAction(event -> showExplore()));
        page.add(back);
        page.add(Box.createVerticalStrut(18));

        JPanel detail = card(new BorderLayout(28, 0));
        detail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        JPanel information = new JPanel();
        information.setOpaque(false);
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));
        information.add(pill(publication.categoria(), TEAL));
        information.add(Box.createVerticalStrut(14));
        information.add(label("<html><div style='width:540px'>" + publication.titulo() + "</div></html>", H1, TEXT));
        information.add(Box.createVerticalStrut(10));
        information.add(label("★ " + publication.reputacion() + "  ·  " + publication.proveedor()
                + "  ·  " + publication.modalidad(), H3, MUTED));
        information.add(Box.createVerticalStrut(22));
        information.add(label("Sobre esta propuesta", H2, TEXT));
        information.add(Box.createVerticalStrut(8));
        information.add(label("<html><div style='width:550px'>" + publication.descripcion() + "</div></html>", BODY, MUTED));
        information.add(Box.createVerticalStrut(16));
        information.add(label("Duración: " + publication.duracionMinutos() + " minutos", BODY, TEXT));
        detail.add(information);

        JPanel requestForm = new Theme.RoundedPanel(16, new Color(239, 247, 246));
        requestForm.setLayout(new BoxLayout(requestForm, BoxLayout.Y_AXIS));
        requestForm.setBorder(new EmptyBorder(22, 22, 22, 22));
        requestForm.add(label("INVERSIÓN", SMALL, MUTED));
        requestForm.add(label(publication.precio() + " SkillCoins", new Font("SansSerif", Font.BOLD, 25), NAVY));
        requestForm.add(Box.createVerticalStrut(8));
        requestForm.add(label("Saldo disponible: " + controllers.wallet().consultarSaldo(USER_ID), SMALL, MUTED));
        requestForm.add(Box.createVerticalStrut(14));
        requestForm.add(label("Franja disponible", H3, TEXT));

        List<FranjaDisponibilidad> availableSlots = publication.franjasDisponibles().stream()
                .filter(FranjaDisponibilidad::disponible)
                .toList();
        JComboBox<FranjaDisponibilidad> slots = new JComboBox<>(availableSlots.toArray(FranjaDisponibilidad[]::new));
        slots.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FranjaDisponibilidad slot) setText(formatSlot(slot));
                return this;
            }
        });
        slots.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        requestForm.add(slots);
        requestForm.add(Box.createVerticalStrut(12));

        JTextField objective = new JTextField();
        objective.setBorder(BorderFactory.createTitledBorder("Objetivo de la mentoría (obligatorio)"));
        objective.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        requestForm.add(objective);
        requestForm.add(Box.createVerticalStrut(8));

        JTextArea clientMessage = new JTextArea(3, 18);
        clientMessage.setLineWrap(true);
        clientMessage.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(clientMessage);
        messageScroll.setBorder(BorderFactory.createTitledBorder("Mensaje para el mentor (opcional)"));
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        requestForm.add(messageScroll);
        requestForm.add(Box.createVerticalStrut(12));

        JButton request = button("Solicitar mentoría", true);
        request.setEnabled(!availableSlots.isEmpty());
        request.addActionListener(uiAction(event -> {
            FranjaDisponibilidad selectedSlot = (FranjaDisponibilidad) slots.getSelectedItem();
            if (objective.getText().isBlank()) {
                message("Falta el objetivo", "Contanos qué querés lograr en la mentoría.");
                return;
            }
            if (selectedSlot == null) {
                message("Sin disponibilidad", "Esta publicación no tiene franjas disponibles.");
                return;
            }
            controllers.contrataciones().solicitar(USER_ID, publication.id(), clientMessage.getText(),
                    objective.getText(), selectedSlot.id());
            message("¡Solicitud enviada!", "Podés seguir su estado desde Contrataciones.");
            showContracts();
        }));
        requestForm.add(request);
        if (availableSlots.isEmpty()) {
            requestForm.add(Box.createVerticalStrut(8));
            requestForm.add(label("No hay franjas con cupos", SMALL, DANGER));
        }

        detail.add(requestForm, BorderLayout.EAST);
        detail.setAlignmentX(LEFT_ALIGNMENT);
        page.add(detail);
        page("Detalle", page);
    }

    private String formatSlot(FranjaDisponibilidad slot) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return slot.inicio().format(format) + " – " + slot.fin().format(DateTimeFormatter.ofPattern("HH:mm"))
                + " · " + slot.cuposDisponibles() + " cupo(s)";
    }

    private void showListings(){JPanel p=base();JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);title.add(label("Gestioná tus publicaciones",H2,TEXT));JButton create=button("+ Nueva publicación",true);create.addActionListener(uiAction(e->createListing()));title.add(create,BorderLayout.EAST);title.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));title.setAlignmentX(LEFT_ALIGNMENT);p.add(title);p.add(Box.createVerticalStrut(15));List<Publicacion> own=controllers.publicaciones().listarPropias(USER_ID);if(own.isEmpty()){p.add(empty("Todavía no publicaste servicios","Creá tu primera propuesta para comenzar a compartir tu talento."));}else for(Publicacion x:own){JPanel row=card(new BorderLayout(12,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));row.add(label(x.titulo(),H3,TEXT));row.add(pill(x.estado().name(),x.estado()==EstadoPublicacion.PUBLICADA?TEAL:MUTED),BorderLayout.CENTER);JButton toggle=button(x.estado()==EstadoPublicacion.PUBLICADA?"Pausar":"Publicar",false);toggle.addActionListener(uiAction(e->{controllers.publicaciones().cambiarEstado(USER_ID,x.id(),x.estado()==EstadoPublicacion.PUBLICADA?EstadoPublicacion.PAUSADA:EstadoPublicacion.PUBLICADA);showListings();}));row.add(toggle,BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(10));}page("Mis publicaciones",p);}

    private void createListing(){JTextField title=new JTextField();JTextArea desc=new JTextArea(4,26);JComboBox<String> cat=new JComboBox<>(controllers.busqueda().categorias().stream().filter(c->!c.equals("Todas")).toArray(String[]::new));JPanel form=new JPanel(new GridLayout(0,1,4,5));form.add(new JLabel("Título"));form.add(title);form.add(new JLabel("Descripción"));form.add(new JScrollPane(desc));form.add(new JLabel("Categoría"));form.add(cat);if(JOptionPane.showConfirmDialog(this,form,"Nueva publicación",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION&&!title.getText().isBlank()){controllers.publicaciones().crearBorrador(USER_ID,title.getText(),desc.getText(),(String)cat.getSelectedItem(),Modalidad.REMOTA,60,150);message("Borrador creado","La publicación quedó lista para que tus alumnos conecten su flujo real.");showListings();}}

    private void showContracts() {
        JPanel p = base();
        addSection(p, "Tus contrataciones", "Los cambios de estado requieren autorización del participante correspondiente");
        for (Contratacion booking : controllers.contrataciones().listarContrataciones(USER_ID)) {
            JPanel row = card(new BorderLayout(16, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(label(booking.titulo(), H3, TEXT));
            text.add(Box.createVerticalStrut(5));
            text.add(label(booking.contraparte() + "  ·  "
                    + booking.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")), BODY, MUTED));
            if (!booking.objetivo().isBlank()) text.add(label("Objetivo: " + booking.objetivo(), SMALL, MUTED));
            if (!booking.mensajeCliente().isBlank()) text.add(label("Mensaje: " + booking.mensajeCliente(), SMALL, MUTED));
            row.add(text);
            row.add(pill(booking.estado().name(), booking.estado() == EstadoContratacion.COMPLETADA ? TEAL : NAVY_2), BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            boolean terminal = booking.estado() == EstadoContratacion.COMPLETADA
                    || booking.estado() == EstadoContratacion.CANCELADA
                    || booking.estado() == EstadoContratacion.RECHAZADA;
            if (!terminal) {
                if (controllers.contrataciones().puedeAvanzar(USER_ID, booking.id())) {
                    JButton next = button("Avanzar estado", true);
                    next.addActionListener(uiAction(event -> {
                        controllers.contrataciones().avanzar(USER_ID, booking.id());
                        showContracts();
                    }));
                    actions.add(next);
                } else if (booking.estado() == EstadoContratacion.PENDIENTE) {
                    actions.add(label("Esperando al mentor", SMALL, MUTED));
                }
                JButton cancel = button("Cancelar", false);
                cancel.addActionListener(uiAction(event -> {
                    controllers.contrataciones().cancelar(USER_ID, booking.id());
                    showContracts();
                }));
                actions.add(cancel);
            }
            if (booking.estado() == EstadoContratacion.COMPLETADA) {
                JButton rate = button("Calificar", true);
                rate.addActionListener(uiAction(event -> showRating(booking)));
                actions.add(rate);
            }
            row.add(actions, BorderLayout.EAST);
            p.add(row);
            p.add(Box.createVerticalStrut(11));
        }
        page("Contrataciones", p);
    }

    private void showRating(Contratacion booking) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        JComboBox<Integer> score = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        JTextField comment = new JTextField();
        panel.add(label("Puntaje", H3, TEXT));
        panel.add(score);
        panel.add(label("Comentario", H3, TEXT));
        panel.add(comment);
        if (JOptionPane.showConfirmDialog(this, panel, "Calificar sesión",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        controllers.calificaciones().calificar(USER_ID, booking.id(), (Integer) score.getSelectedItem(), comment.getText());
        JOptionPane.showMessageDialog(this, "Calificación registrada");
        showContracts();
    }

    private void showWallet(){JPanel p=base();JPanel balance=new Theme.RoundedPanel(22,NAVY_2);balance.setLayout(new BorderLayout());balance.setBorder(new EmptyBorder(24,26,24,26));balance.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));JPanel btxt=new JPanel();btxt.setOpaque(false);btxt.setLayout(new BoxLayout(btxt,BoxLayout.Y_AXIS));btxt.add(label("SALDO DISPONIBLE",SMALL,new Color(184,209,222)));btxt.add(label(controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",new Font("SansSerif",Font.BOLD,30),Color.WHITE));balance.add(btxt);JButton add=button("+ Acreditar demo",true);add.addActionListener(uiAction(e->{controllers.wallet().acreditarDemo(USER_ID,500);showWallet();}));balance.add(add,BorderLayout.EAST);balance.setAlignmentX(LEFT_ALIGNMENT);p.add(balance);p.add(Box.createVerticalStrut(24));addSection(p,"Movimientos recientes",null);for(Movimiento m:controllers.wallet().listarMovimientos(USER_ID)){JPanel row=card(new BorderLayout());row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(m.concepto(),H3,TEXT));t.add(label(m.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")),SMALL,MUTED));row.add(t);row.add(label((m.importe()>0?"+":"")+m.importe()+" ◈",H3,m.importe()>0?TEAL:DANGER),BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(8));}page("SkillCoins",p);}

    private void showNotifications(){JPanel p=base();JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.add(label("Centro de notificaciones",H2,TEXT));JButton all=button("Marcar todas como leídas",false);all.addActionListener(uiAction(e->{controllers.notificaciones().marcarTodasLeidas(USER_ID);showNotifications();}));head.add(all,BorderLayout.EAST);head.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));head.setAlignmentX(LEFT_ALIGNMENT);p.add(head);p.add(Box.createVerticalStrut(15));for(Notificacion n:controllers.notificaciones().listarNotificaciones(USER_ID)){JPanel row=card(new BorderLayout(14,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));JLabel dot=label(n.leida()?"○":"●",H2,n.leida()?BORDER:TEAL);row.add(dot,BorderLayout.WEST);JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(n.titulo(),H3,TEXT));t.add(label(n.detalle(),BODY,MUTED));t.add(label(n.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")),SMALL,MUTED));row.add(t);if(!n.leida()){JButton read=button("Marcar leída",false);read.addActionListener(uiAction(e->{controllers.notificaciones().marcarLeida(USER_ID,n.id());showNotifications();}));row.add(read,BorderLayout.EAST);}p.add(row);p.add(Box.createVerticalStrut(9));}page("Notificaciones",p);}

    private void showProfile(){Perfil x=controllers.perfiles().verPerfil(USER_ID);JPanel p=base();JPanel hero=card(new BorderLayout(20,0));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,180));hero.add(avatar("MS",86),BorderLayout.WEST);JPanel data=new JPanel();data.setOpaque(false);data.setLayout(new BoxLayout(data,BoxLayout.Y_AXIS));data.add(label(x.nombre(),H1,TEXT));data.add(label(x.rol(),H3,TEAL_DARK));data.add(Box.createVerticalStrut(10));data.add(label("<html><div style='width:570px'>"+x.bio()+"</div></html>",BODY,MUTED));hero.add(data);JPanel stats=new JPanel(new GridLayout(3,1,0,4));stats.setOpaque(false);stats.add(label("★ "+x.reputacion()+" reputación",H3,GOLD));stats.add(label(x.resenas()+" reseñas",BODY,MUTED));stats.add(label(x.proyectosCompletados()+" proyectos",BODY,MUTED));hero.add(stats,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(24));addSection(p,"Habilidades",null);JPanel tags=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));tags.setOpaque(false);for(String s:x.habilidades())tags.add(pill(s,NAVY_2));tags.setAlignmentX(LEFT_ALIGNMENT);p.add(tags);page("Mi perfil",p);}

    private JLabel pill(String text,Color color){JLabel l=label(text,new Font("SansSerif",Font.BOLD,10),Color.WHITE);l.setOpaque(true);l.setBackground(color);l.setBorder(new EmptyBorder(6,9,6,9));return l;}
    private JLabel avatar(String initials,int size){JLabel a=new JLabel(initials,SwingConstants.CENTER);a.setPreferredSize(new Dimension(size,size));a.setMinimumSize(new Dimension(size,size));a.setMaximumSize(new Dimension(size,size));a.setOpaque(true);a.setBackground(TEAL);a.setForeground(Color.WHITE);a.setFont(new Font("SansSerif",Font.BOLD,size/3));a.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,100),2,true));return a;}
    private JComponent empty(String title,String subtitle){JPanel e=card(new GridBagLayout());e.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));JLabel h=label(title,H2,TEXT);h.setAlignmentX(CENTER_ALIGNMENT);t.add(h);t.add(Box.createVerticalStrut(8));JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(CENTER_ALIGNMENT);t.add(s);e.add(t);return e;}
    private ActionListener uiAction(ActionListener listener) {
        return event -> actions.run(() -> listener.actionPerformed(event));
    }

    private void message(String title,String body){JOptionPane.showMessageDialog(this,body,title,JOptionPane.INFORMATION_MESSAGE);}
}
