export type SlotId = 'tl' | 'tr' | 'bl' | 'br';

export type SlotMap = Record<SlotId, number | null>;

export interface DashboardRegion {
  id: string;
  slots: SlotId[];
  panelId: number | null;
  label: string;
  gridColumn: string;
  gridRow: string;
}

const SLOT_LABEL: Record<SlotId, string> = {
  tl: 'Esquerda superior',
  tr: 'Direita superior',
  bl: 'Esquerda inferior',
  br: 'Direita inferior',
};

const CELL: Record<SlotId, { gridColumn: string; gridRow: string }> = {
  tl: { gridColumn: '1', gridRow: '1' },
  tr: { gridColumn: '2', gridRow: '1' },
  bl: { gridColumn: '1', gridRow: '2' },
  br: { gridColumn: '2', gridRow: '2' },
};

const samePanel = (slots: SlotMap, a: SlotId, b: SlotId): boolean =>
  slots[a] != null && slots[a] === slots[b];

export const emptySlots = (): SlotMap => ({
  tl: null,
  tr: null,
  bl: null,
  br: null,
});

export const buildDashboardRegions = (slots: SlotMap): DashboardRegion[] => {
  if (
    samePanel(slots, 'tl', 'tr') &&
    samePanel(slots, 'tl', 'bl') &&
    samePanel(slots, 'tl', 'br')
  ) {
    return [
      {
        id: 'all',
        slots: ['tl', 'tr', 'bl', 'br'],
        panelId: slots.tl,
        label: 'Dashboard',
        gridColumn: '1 / 3',
        gridRow: '1 / 3',
      },
    ];
  }

  const consumed = new Set<SlotId>();
  const regions: DashboardRegion[] = [];

  const addPair = (a: SlotId, b: SlotId, gridColumn: string, gridRow: string) => {
    if (!samePanel(slots, a, b) || consumed.has(a) || consumed.has(b)) {
      return;
    }
    consumed.add(a);
    consumed.add(b);
    regions.push({
      id: `${a}-${b}`,
      slots: [a, b],
      panelId: slots[a],
      label: `${SLOT_LABEL[a]} e ${SLOT_LABEL[b]}`,
      gridColumn,
      gridRow,
    });
  };

  addPair('tl', 'tr', '1 / 3', '1');
  addPair('bl', 'br', '1 / 3', '2');
  addPair('tl', 'bl', '1', '1 / 3');
  addPair('tr', 'br', '2', '1 / 3');

  (['tl', 'tr', 'bl', 'br'] as SlotId[]).forEach((slot) => {
    if (consumed.has(slot)) {
      return;
    }
    regions.push({
      id: slot,
      slots: [slot],
      panelId: slots[slot],
      label: SLOT_LABEL[slot],
      gridColumn: CELL[slot].gridColumn,
      gridRow: CELL[slot].gridRow,
    });
  });

  return regions;
};
