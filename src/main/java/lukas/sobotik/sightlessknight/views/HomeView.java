package lukas.sobotik.sightlessknight.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("SightlessKnight")
@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout {
    HomeView() {
        H1 practiceMoves = new H1("Practice Moves");
        Button knightButton = new Button("Knight");
        knightButton.addClickListener(e -> {
            knightButton.getUI().ifPresent(ui ->
                    ui.navigate("play/knight"));
        });

        add(practiceMoves, knightButton);
    }
}