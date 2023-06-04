package com.tvz.java.jamb.model;

import com.tvz.java.jamb.YambController;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState implements Serializable {
    private int[] dobiveniBrojevi = new int[YambController.BROJ_KOCKICA];

    private int brojBacanja;

    private int BROJAC_GORE;

    private int BROJAC_DOLJE;

    private int[][] GAME_BOARD = new int[YambController.BROJ_MOGUCIH_POLJA][YambController.BROJ_KOLONA];
}
