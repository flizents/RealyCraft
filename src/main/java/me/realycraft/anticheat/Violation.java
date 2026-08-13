package me.realycraft.anticheat;

import java.time.Instant;
public record Violation(CheckType check, double addedVl, String detail, Instant time) { }