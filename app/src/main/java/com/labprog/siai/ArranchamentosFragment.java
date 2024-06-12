package com.labprog.siai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArranchamentosFragment extends Fragment {

    private TextView textViewRefeicoes;
    private TableLayout tableLayoutCardapio;

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arranchamentos, container, false);
        textViewRefeicoes = view.findViewById(R.id.textViewRefeicoes);
        tableLayoutCardapio = view.findViewById(R.id.tableLayoutCardapio);
        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuActivity activity = (MenuActivity) getActivity();
        if (activity != null) {
            List<String> arranchamentosHoje = activity.getArranchamentosHoje();
            StringBuilder refeicoesBuilder = new StringBuilder();
            if (arranchamentosHoje.isEmpty()) {
                refeicoesBuilder.append("Você não está arranchado para nenhuma refeição hoje.\n");
            } else {
                for (String refeicao : arranchamentosHoje) {
                    refeicoesBuilder.append(refeicao).append("\n");
                }
            }
            textViewRefeicoes.setText(refeicoesBuilder.toString());
            textViewRefeicoes.setTextSize(24);
            textViewRefeicoes.setTextColor(getResources().getColor(android.R.color.white));

            // Adding menu items
            addMenuItem("Café", "(CPLM), Mortadela, Frutas");
            addMenuItem("Almoço", "Arroz, Feijão, Salmão ao molho de maracujá, Salada");
            addMenuItem("Janta", "Arroz, Estrogonofe, Salada");
            addMenuItem("Ceia", "Cachorro-Quente");
        }
    }

    private void addMenuItem(String meal, String menu) {
        TableRow tableRow = new TableRow(getContext());
        tableRow.setBackgroundResource(R.drawable.table_border); // Apply the border drawable

        TextView mealTextView = new TextView(getContext());
        mealTextView.setText(meal);
        mealTextView.setPadding(8, 8, 8, 8);
        mealTextView.setTextColor(getResources().getColor(android.R.color.white)); // Set text color to white

        TextView menuTextView = new TextView(getContext());
        menuTextView.setText(menu);
        menuTextView.setPadding(8, 8, 8, 8);
        menuTextView.setTextColor(getResources().getColor(android.R.color.white)); // Set text color to white

        tableRow.addView(mealTextView);
        tableRow.addView(menuTextView);
        tableLayoutCardapio.addView(tableRow);
    }
}
