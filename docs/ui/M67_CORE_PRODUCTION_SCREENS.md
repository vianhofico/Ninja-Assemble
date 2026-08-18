# M67 Core Production Screens

M67 productionizes Home, Ninja Roster, Hero Detail, Formation, Campaign/Adventure, Summon and Inventory/Equipment without duplicating the working store/controller business logic.

A runtime installer replaces the generic body panel with M66 production primitives, mirrors live body/status data from the existing controller and forwards the production CTA to the existing authoritative action. This lets the product UI change while keeping battle, progression, summon, inventory and formation state paths single-sourced.

The generic controller remains a compatibility/data adapter until M68 and later UI cleanup can retire it safely.
