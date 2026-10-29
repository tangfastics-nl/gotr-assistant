package com.pbjwz.gotrassistant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Alignment
{
	ELEMENTAL("Elemental"),
	CATALYTIC("Catalytic");

	private final String label;
}
