package com.trutgame.server.domain.model;

public record TokenCount(int grands, int petits) {

    public static TokenCount zero() {
        return new TokenCount(0, 0);
    }

    public TokenCount addPetit() {
        int newPetits = petits + 1;
        int newGrands = grands;
        if (newPetits >= 3) {
            newGrands++;
            newPetits -= 3;
        }
        return new TokenCount(newGrands, newPetits);
    }

    public TokenCount addGrand() {
        return new TokenCount(grands + 1, petits);
    }

    public TokenCount losePetits() {
        return new TokenCount(grands, 0);
    }

    public boolean isFortial() {
        return grands == 6 && petits == 2;
    }

    public boolean hasWon() {
        return grands >= 7;
    }

    public int totalGrands() {
        return grands;
    }
}
