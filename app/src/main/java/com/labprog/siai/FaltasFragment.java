package com.labprog.siai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FaltasFragment extends Fragment {

    private TextView textViewFaltasTotal;
    private LinearLayout linearLayoutFaltas;

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faltas, container, false);
        textViewFaltasTotal = view.findViewById(R.id.textViewFaltasTotal);
        linearLayoutFaltas = view.findViewById(R.id.linearLayoutFaltas);
        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuActivity activity = (MenuActivity) getActivity();
        if (activity != null) {
            List<String> faltasLista = activity.getfaltasLista();
            int faltasUsuario = activity.getFaltasUsuario();
            String faltasText = "Faltas no rancho até ontem: " + faltasLista.size() / 2;
            textViewFaltasTotal.setText(faltasText);



            // Remover duplicatas usando um LinkedHashSet para manter a ordem
            Set<String> faltasSet = new LinkedHashSet<>(faltasLista);

            // Reverse the list to show from last to first
            for (String falta : faltasSet) {
                String formattedFalta = formatFalta(falta);
                addFaltaToLayout(formattedFalta);
            }
        }
    }

    private void addFaltaToLayout(String formattedFalta) {
        TextView textViewFalta = new TextView(getContext());
        textViewFalta.setText(formattedFalta);
        textViewFalta.setTextSize(24);
        textViewFalta.setTextColor(getResources().getColor(android.R.color.white)); // Set text color to white
        textViewFalta.setPadding(8, 8, 8, 8);
        linearLayoutFaltas.addView(textViewFalta);
    }

    private String formatFalta(String falta) {
        String[] parts = falta.split("_");
        String date = parts[0];
        String tipo = parts[1];
        return date + ": " + tipo;
    }
}
