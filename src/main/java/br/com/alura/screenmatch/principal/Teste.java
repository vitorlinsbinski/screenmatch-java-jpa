package br.com.alura.screenmatch.principal;

import java.sql.SQLOutput;

public class Teste {
    public static void main(String[] args) {
        for(Nivel nivel : Nivel.values()) {
            System.out.println("Nível: " + nivel + ", Pontuação: " + nivel.getPontuacao());
        }

        Nivel nivelIniciante = Nivel.INICIANTE;
        System.out.println("Pontuação do nível iniciante: " + nivelIniciante.getPontuacao());
    }
}

enum Nivel {
    INICIANTE(1),
    INTERMEDIARIO(2),
    AVANCADO(3);

    private int pontuacao;
    Nivel(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public int getPontuacao() {
        return this.pontuacao;
    }
}